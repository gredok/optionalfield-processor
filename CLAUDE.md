# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.gredok.optionalfield.processor.OptionalFieldClassDeserializerTest2"

# Run a single test method
./gradlew test --tests "com.gredok.optionalfield.processor.OptionalFieldClassDeserializerTest2.shouldDeserializeJsonWithMissingFields"

# Publish to local Maven repository
./gradlew publishToMavenLocal

# Publish to Maven Central (requires Central Portal + signing credentials, see settings.gradle.kts)
./gradlew publishAggregationToCentralPortal
```

## Architecture

This is a Java annotation processor library (Java 21) that solves the PATCH request problem: distinguishing between a field that was **not sent** in a JSON payload vs a field that was **explicitly set to null**.

### Core concept

The `OptionalField<T>` wrapper class carries three pieces of information:
- `fieldName` — the JSON key the value was read from (honors `@JsonProperty`'s custom name when present; falls back to the Java field name otherwise). Note this is distinct from `toMap()`'s output keys, which always use the Java field name (see `toMapShouldUseJavaFieldNameNotJsonPropertyName`) — the two are independent by design.
- `isPresent` — whether the field appeared in the JSON
- `value` — the deserialized value (may be null even when present)

### Code generation flow

1. Annotate a plain POJO with `@OptionalClassReq`
2. `OptionalFieldProcessor` (a `javax.annotation.processing.AbstractProcessor`) picks it up at compile time
3. It generates a `<ClassName>Req` class where every field is wrapped in `OptionalField<T>`
4. The generated class includes: getters, setters, a builder, and inner `Deserializer`/`Serializer` classes that extend the abstract Jackson helpers

### Jackson integration

- `AbstractOptionalFieldClassDeserializer<T>` — uses reflection to walk the JSON tree; marks each field as present/absent based on whether it appears in the JSON object, regardless of null. The JSON key looked up per field honors `@JsonProperty`'s custom name, matching the serializer. Unknown JSON properties are reported through `DeserializationContext.handleUnknownProperty` (with an explicit `FAIL_ON_UNKNOWN_PROPERTIES` fallback check), so unknown-property handling behaves the same as with a regular Jackson-generated deserializer. Fields typed with nested generics (e.g. `List<List<Dto>>`) are deserialized against the fully-resolved `JavaType` directly, so nested elements come back as real DTOs rather than `LinkedHashMap`.
- `AbstractOptionalFieldClassSerializer<T>` — only writes fields whose `OptionalField.isPresent()` is true, respecting `@JsonProperty` for custom names

The processor targets Jackson 3.x (`tools.jackson.*` package) while the serializer also imports `com.fasterxml.jackson.annotation.JsonProperty` for annotation inspection.

**Remaining asymmetry:** only `@JsonProperty`'s custom name is honored on read. `@JsonAlias`, a configured naming strategy, `@JsonIgnore`, and read/write-only access modes are not — tracked in ROADMAP.md.

**Empty strings are preserved as `""`**, not coerced to `null` — for PATCH APIs the two commonly represent distinct updates.

**Serializer custom dispatch:** if a source field carries `@JsonSerialize(using=...)`, the processor forwards that annotation to the generated field, and `AbstractOptionalFieldClassSerializer` instantiates that serializer reflectively.

### Bean Validation integration

`com.gredok.optionalfield.processor.validation.OptionalFieldValueExtractor` registers `OptionalField<T>` as a Bean Validation container type via `META-INF/services/jakarta.validation.valueextraction.ValueExtractor`. Validation annotations forwarded onto the type argument (e.g. `OptionalField<@NotNull String>`, per the field-vs-type-annotation handling below) are applied to the unwrapped value rather than to the `OptionalField` instance itself. Without this extractor, Bean Validation providers throw `ConstraintDeclarationException` instead of validating.

### Annotation handling in the processor

- `@Schema` (OpenAPI) annotations are in `skipAnnotations` and are silently dropped from generated fields.
- `io.swagger.v3` imports are also filtered from the generated file's import list.
- `@JsonProperty`, `@JsonSerialize`, `@JsonDeserialize` are treated as field-level (not type-level) annotations and placed before `OptionalField<>` rather than inside the type argument.

### Processor registration

The processor is registered via the standard service-loader mechanism at:
`src/main/resources/META-INF/services/javax.annotation.processing.Processor`

### Test setup

Tests use classes in `src/test/java` as annotation processor inputs. The build wires `testAnnotationProcessor(project(":"))` so the processor runs on test sources — meaning generated `*Req` classes (e.g. `TestJsonReq`, `TestJson2Req`) are produced during test compilation and used directly in tests. `Test2` is a manually written equivalent to show the pattern without code generation.