package io.optionalfield.processor;

public class OptionalField<T> {
    private String fieldName = "-";
    private boolean isPresent = false;
    private T value;

    public OptionalField() {
    }

    public OptionalField(String fieldName, boolean isPresent, T value) {
        this.fieldName = fieldName;
        this.isPresent = isPresent;
        this.value = value;
    }

    public static <T> OptionalField<T> empty() {
        return new OptionalField<>();
    }

    public static <T> OptionalField<T> of(T value) {
        return new OptionalField<>("-", true, value);
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public T getValue() {
        return value;
    }
}
