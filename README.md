# OptionalField Processor

A Java annotation processor that solves the **PATCH request problem**: distinguishing between a field that was **not sent** in a JSON payload and a field that was **explicitly set to null**.

## The problem

With standard Jackson deserialization, you can't tell the difference between:
```json
{ "name": "Alice" }          // note was not sent
{ "name": "Alice", "note": null }  // note was explicitly nulled
```

Both result in `note == null`. For PATCH endpoints, this matters — you should only update fields that were actually sent.

## How it works

Annotate a plain POJO with `@OptionalClassReq`:

```java
@OptionalClassReq
public class UpdateUserRequest {
    private String name;
    private String note;
}
```

The annotation processor generates `UpdateUserRequestReq` at compile time, where every field is wrapped in `OptionalField<T>`:

```java
// Generated class (simplified)
@JsonDeserialize(using = UpdateUserRequestReq.Deserializer.class)
@JsonSerialize(using = UpdateUserRequestReq.Serializer.class)
public class UpdateUserRequestReq {
    private OptionalField<String> name = new OptionalField<>("name", false, null);
    private OptionalField<String> note = new OptionalField<>("note", false, null);
    // getters, setters, builder, Deserializer, Serializer ...
}
```

Use it in your controller:

```java
@PatchMapping("/users/{id}")
public void updateUser(@PathVariable Long id, @RequestBody UpdateUserRequestReq req) {
    if (req.getName().isPresent()) {
        user.setName(req.getName().getValue()); // may be null
    }
    if (req.getNote().isPresent()) {
        user.setNote(req.getNote().getValue()); // may be null
    }
}
```

`OptionalField<T>` carries three pieces of information:
- `isPresent()` — whether the field appeared in the JSON at all
- `getValue()` — the deserialized value (may be `null` even when present)
- `getFieldName()` — the field name

The serializer only writes fields where `isPresent()` is `true`, so serializing a partially-filled `*Req` object produces a minimal JSON patch document.

## Installation

### Gradle

```kotlin
dependencies {
    implementation("io.optionalfield:optionalfield-processor:1.2.0")
    annotationProcessor("io.optionalfield:optionalfield-processor:1.2.0")
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/jgreznar/optionalfield-processor")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### Maven

```xml
<dependency>
    <groupId>io.optionalfield</groupId>
    <artifactId>optionalfield-processor</artifactId>
    <version>1.2.0</version>
</dependency>
```

## Requirements

- Java 21+
- Jackson 3.x (`tools.jackson.*`)

## Annotations forwarded to generated class

Field-level annotations (`@JsonProperty`, `@JsonSerialize`, `@JsonDeserialize`) are forwarded to the generated class. Jakarta Validation annotations (e.g. `@NotNull`) are placed inside the type argument `OptionalField<@NotNull String>`. OpenAPI `@Schema` annotations are dropped.

## Builder

The generated class includes a builder:

```java
UpdateUserRequestReq req = UpdateUserRequestReq.builder()
    .name("Alice")   // marks name as present
    .build();
// req.getNote().isPresent() == false
```

## License

MIT
