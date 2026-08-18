package com.gredok.optionalfield.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;

/** Regression specifications for behavior that is not covered by the original test suite. */
class OptionalFieldRegressionTest {

    @OptionalClassReq
    static class RenamedPropertyModel {
        @JsonProperty("external_name")
        private String name;
    }

    @OptionalClassReq
    static class NestedCollectionModel {
        private List<List<Test22>> groups;
    }

    @OptionalClassReq
    static class StaticFieldModel {
        private static final String KIND = "fixed";
        private String name;
    }

    private ObjectMapper lenientMapper() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.USE_ANNOTATIONS)
                .annotationIntrospector(new JacksonAnnotationIntrospector())
                .findAndAddModules()
                .build();
    }

    @Test
    void shouldDeserializeUsingJsonPropertyName() {
        String json = """
                {
                    "external_name": "Alice"
                }
                """;

        RenamedPropertyModelReq result =
                lenientMapper().readValue(json, RenamedPropertyModelReq.class);

        assertTrue(result.getName().isPresent());
        assertEquals("external_name", result.getName().getFieldName());
        assertEquals("Alice", result.getName().getValue());
    }

    @Test
    void shouldPreserveNestedCollectionElementTypes() {
        String json = """
                {
                    "groups": [
                        [
                            {"name": "first", "note": "nested"}
                        ]
                    ]
                }
                """;

        NestedCollectionModelReq result =
                lenientMapper().readValue(json, NestedCollectionModelReq.class);

        assertTrue(result.getGroups().isPresent());
        Test22 nested = result.getGroups().getValue().getFirst().getFirst();
        assertEquals("first", nested.getName());
        assertEquals("nested", nested.getNote());
    }

    @Test
    void shouldNotGenerateRequestPropertiesForStaticFields() {
        boolean containsStaticSourceField = Arrays.stream(StaticFieldModelReq.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("KIND"));

        assertFalse(containsStaticSourceField,
                "Static constants must not become mutable request properties");
    }
}
