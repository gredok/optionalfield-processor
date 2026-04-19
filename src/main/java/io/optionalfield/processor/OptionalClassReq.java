package io.optionalfield.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a POJO for compile-time generation of a corresponding {@code *Req} class.
 *
 * <p>The annotation processor generates a class named {@code <OriginalClass>Req} in the same
 * package, where every field is wrapped in {@link OptionalField OptionalField&lt;T&gt;}. The
 * generated class includes getters, setters, a builder, and inner {@code Deserializer} /
 * {@code Serializer} classes wired to Jackson automatically via {@code @JsonDeserialize} /
 * {@code @JsonSerialize}.
 *
 * <pre>{@code
 * @OptionalClassReq
 * public class UpdateUserRequest {
 *     private String name;
 *     private String note;
 * }
 * // Generates: UpdateUserRequestReq with OptionalField<String> name, note
 * }</pre>
 *
 * <p>Field annotations are forwarded to the generated class:
 * <ul>
 *   <li>{@code @JsonProperty}, {@code @JsonSerialize}, {@code @JsonDeserialize} — placed at
 *       field level</li>
 *   <li>Jakarta Validation annotations (e.g. {@code @NotNull}) — placed inside the type
 *       argument: {@code OptionalField<@NotNull String>}</li>
 *   <li>{@code @Schema} (OpenAPI) — dropped</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalClassReq {
}
