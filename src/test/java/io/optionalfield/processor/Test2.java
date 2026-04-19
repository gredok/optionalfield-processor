package io.optionalfield.processor;

import io.optionalfield.processor.jackson.AbstractOptionalFieldClassDeserializer;
import io.optionalfield.processor.jackson.AbstractOptionalFieldClassSerializer;

import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Data @JsonDeserialize(using = Test2.Test2Deserializer.class) @JsonSerialize(using = Test2.Test2Serializer.class)
public class Test2 {
    private OptionalField<String> name;
    private OptionalField<Test22> note;

    public static class Test2Deserializer extends AbstractOptionalFieldClassDeserializer<Test2> {
        @Override protected Class<Test2> rawClass() {
            return Test2.class;
        }
    }

    public static class Test2Serializer extends AbstractOptionalFieldClassSerializer<Test2> {
    }

}
