package com.gredok.optionalfield.processor;

import java.util.Objects;

/**
 * Wraps a single field value to carry presence information alongside the value itself.
 *
 * <p>This solves the PATCH request problem: standard JSON deserialization cannot
 * distinguish between a field that was omitted from the payload and a field that
 * was explicitly set to {@code null}. {@code OptionalField} records both cases.
 *
 * <pre>{@code
 * // Field present with a value
 * OptionalField<String> name = OptionalField.of("Alice");
 * name.isPresent(); // true
 * name.getValue();  // "Alice"
 *
 * // Field present but null
 * OptionalField<String> note = new OptionalField<>("note", true, null);
 * note.isPresent(); // true
 * note.getValue();  // null
 *
 * // Field absent (not sent in the payload)
 * OptionalField<String> tag = OptionalField.empty();
 * tag.isPresent(); // false
 * }</pre>
 *
 * @param <T> the type of the wrapped value
 */
public class OptionalField<T> {
    private String fieldName = "-";
    private boolean isPresent = false;
    private T value;

    /** Creates an absent field with no name or value. */
    public OptionalField() {
    }

    /**
     * Creates an {@code OptionalField} with explicit presence and value.
     *
     * @param fieldName  the JSON field name
     * @param isPresent  {@code true} if the field appeared in the JSON payload
     * @param value      the deserialized value; may be {@code null} even when present
     */
    public OptionalField(String fieldName, boolean isPresent, T value) {
        this.fieldName = fieldName;
        this.isPresent = isPresent;
        this.value = value;
    }

    /**
     * Returns an absent {@code OptionalField} — equivalent to a field not sent in the payload.
     *
     * @param <T> the value type
     * @return an absent field
     */
    public static <T> OptionalField<T> empty() {
        return new OptionalField<>();
    }

    /**
     * Returns a present {@code OptionalField} with the given value.
     * Useful for constructing request objects programmatically.
     *
     * @param <T>   the value type
     * @param value the value; may be {@code null}
     * @return a present field wrapping {@code value}
     */
    public static <T> OptionalField<T> of(T value) {
        return new OptionalField<>("-", true, value);
    }

    /**
     * Returns the JSON field name this instance was deserialized from.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns {@code true} if the field appeared in the JSON payload, regardless of whether
     * its value is {@code null}.
     *
     * @return {@code true} if present
     */
    public boolean isPresent() {
        return isPresent;
    }

    /**
     * Returns the deserialized value. May be {@code null} even when {@link #isPresent()} is {@code true}.
     *
     * @return the value, or {@code null}
     */
    public T getValue() {
        return value;
    }

    /**
     * Two {@code OptionalField} instances are equal when they have the same field name,
     * presence flag, and value.
     *
     * @param o the object to compare against
     * @return {@code true} if equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OptionalField<?> that)) return false;
        return isPresent == that.isPresent
                && Objects.equals(fieldName, that.fieldName)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, isPresent, value);
    }

    @Override
    public String toString() {
        return "OptionalField{fieldName='" + fieldName + "', isPresent=" + isPresent + ", value=" + value + '}';
    }
}
