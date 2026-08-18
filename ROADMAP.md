# Roadmap

Larger extensions that are worth doing but too big to bundle into a single bugfix pass.
Roughly in priority order.

## 1. Compile-testing coverage for generated source

The processor is currently only tested indirectly, through hand-written classes annotated with
`@OptionalClassReq` and assertions against their generated `*Req` output at runtime. There's no
coverage that directly inspects the *generated source* for correctness across trickier inputs:
static fields, generic fields (`List<Foo>`, nested generics), fields with type-use annotation
targets, array-typed fields, nested/inner source types, and fields whose required imports collide
(e.g. two same-simple-name types from different packages). A `compile-testing`-style harness
(e.g. Google's `compile-testing`, or a hand-rolled `JavaCompiler` + `Filer` capture) would let
tests assert against the emitted `.java` source directly, independent of whether it happens to
also compile and run correctly.

## 2. `@JsonAlias` / naming-strategy / `@JsonIgnore` / access-mode support

The deserializer now honors `@JsonProperty`'s custom name on read (matching the serializer), but
there's still no support for `@JsonAlias` (multiple accepted input names), a configured
`PropertyNamingStrategy`, `@JsonIgnore` (an ignored field still round-trips), or Jackson's
read-only/write-only access modes. All of this needs to land together, since deserializer and
serializer field-name resolution has to keep agreeing.

## 3. Generate direct (de)serializer access instead of reflection

`AbstractOptionalFieldClassDeserializer`/`AbstractOptionalFieldClassSerializer` walk fields via
`java.lang.reflect.Field` at runtime. Since the processor already knows every field statically at
compile time, it could instead generate direct getter/setter calls (or a generated
`serialize`/`deserialize` body per class) — better performance, and better compatibility with the
Java Platform Module System (reflection over non-open packages requires `opens`/`--add-opens`,
which direct method calls don't).

## 4. Support inherited fields and records, or explicitly diagnose unsupported models

The processor only looks at `classElement.getEnclosedElements()` — fields declared on a
superclass are silently skipped rather than included or rejected with a clear compiler error.
Records aren't supported at all (no mutable fields to reflect over the same way). Either extend
the processor to walk the supertype chain and support record components, or have it emit a clear
`Diagnostic.Kind.ERROR` for inputs it can't handle instead of generating incomplete or broken
output.
