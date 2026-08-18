package com.gredok.optionalfield.processor.validation;

import com.gredok.optionalfield.processor.OptionalField;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

/**
 * Registers {@link OptionalField} as a Bean Validation container type.
 *
 * <p>{@code @OptionalClassReq} forwards Jakarta Validation annotations onto the wrapped type
 * argument, e.g. {@code OptionalField<@NotNull String>}. Without a registered
 * {@code ValueExtractor} for {@code OptionalField}, Bean Validation providers such as Hibernate
 * Validator have no way to resolve that container-element constraint and throw
 * {@code ConstraintDeclarationException} instead of validating the wrapped value. This extractor
 * unwraps {@code OptionalField} so the constraint is applied to {@link OptionalField#getValue()}.
 *
 * <p>Only <em>present</em> fields are extracted. An absent field ({@link OptionalField#isPresent()}
 * {@code == false}) means the field was never sent — it has nothing to validate and must not be
 * treated as {@code null} for the purposes of a container-element {@code @NotNull} constraint,
 * otherwise every PATCH payload that omits an optional field would fail validation.
 *
 * <p>Discovered automatically via the standard {@code java.util.ServiceLoader} mechanism at
 * {@code META-INF/services/jakarta.validation.valueextraction.ValueExtractor} — no manual
 * registration required as long as a Bean Validation provider is on the classpath.
 */
public class OptionalFieldValueExtractor implements ValueExtractor<OptionalField<@ExtractedValue ?>> {

    @Override
    public void extractValues(OptionalField<?> originalValue, ValueReceiver receiver) {
        if (originalValue != null && originalValue.isPresent()) {
            receiver.value(null, originalValue.getValue());
        }
    }
}
