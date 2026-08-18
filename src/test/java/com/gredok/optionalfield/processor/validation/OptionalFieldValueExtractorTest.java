package com.gredok.optionalfield.processor.validation;

import static org.junit.jupiter.api.Assertions.*;

import com.gredok.optionalfield.processor.OptionalClassReq;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link OptionalFieldValueExtractor} lets Bean Validation see through
 * {@code OptionalField<T>} and validate the wrapped value, rather than throwing
 * {@code ConstraintDeclarationException} for an unresolvable container-element constraint.
 */
public class OptionalFieldValueExtractorTest {

    @OptionalClassReq
    public static class ValidatedThing {
        @NotNull
        private String name;
    }

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void shouldFlagPresentNullValueAsConstraintViolation() {
        ValidatedThingReq req = ValidatedThingReq.builder().name(null).build();

        Set<ConstraintViolation<ValidatedThingReq>> violations = validator.validate(req);

        assertEquals(1, violations.size());
    }

    @Test
    public void shouldPassWhenValuePresent() {
        ValidatedThingReq req = ValidatedThingReq.builder().name("Alice").build();

        Set<ConstraintViolation<ValidatedThingReq>> violations = validator.validate(req);

        assertTrue(violations.isEmpty());
    }

    @Test
    public void shouldPassWhenFieldAbsent() {
        // Absent (never set) — the @NotNull constraint targets the wrapped value, not the
        // OptionalField wrapper itself, so an absent field has nothing to validate.
        ValidatedThingReq req = new ValidatedThingReq();

        Set<ConstraintViolation<ValidatedThingReq>> violations = validator.validate(req);

        assertTrue(violations.isEmpty());
    }
}
