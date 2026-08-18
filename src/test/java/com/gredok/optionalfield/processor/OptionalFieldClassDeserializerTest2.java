package com.gredok.optionalfield.processor;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;

public class OptionalFieldClassDeserializerTest2 {


    public ObjectMapper testObjectMapper() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(MapperFeature.USE_ANNOTATIONS)
                .annotationIntrospector(new JacksonAnnotationIntrospector())
                .findAndAddModules()
                .build();
    }

        @OptionalClassReq
        public static class TestJson {
            private String name;
            private String note;
        }

    @OptionalClassReq
    public static class TestJson2 {
        @JsonProperty("name_1")
        private String name;
        private boolean note;
    }




    @Test
    public void shouldDeserializeJsonWithMissingFields() {
        String json = """
                    {
                        "name": "outer name"
                    }
                """;

        Test2 result = testObjectMapper().readValue(json, Test2.class);

        assertTrue(result.getName().isPresent());
        assertEquals("outer name", result.getName().getValue());

        assertFalse(result.getNote().isPresent());

        String serialized = testObjectMapper().writeValueAsString(result);

        // The serializer should NOT output "note"
        assertTrue(serialized.contains("\"name\":\"outer name\""));
        assertFalse(serialized.contains("note"));
    }

    @Test
    public void shouldDeserializeJsonWithMissingOptionalFieldsInNestedObject() {
        String json = """
                    {
                        "name": "outer name",
                        "note": {
                            "name": "nested name"
                        }
                    }
                """;

        Test2 result = testObjectMapper().readValue(json, Test2.class);

        assertTrue(result.getName().isPresent());
        assertEquals("outer name", result.getName().getValue());

        assertTrue(result.getNote().isPresent());
        Test22 nestedResult = result.getNote().getValue();
        assertEquals("nested name", nestedResult.getName());
        assertNull(nestedResult.getNote());
    }


    @Test
    public void shouldDeserializeJsonWithMissingFieldsReqObject() {
        String json = """
                    {
                        "name": "outer name"
                    }
                """;

        TestJsonReq result = testObjectMapper().readValue(json, TestJsonReq.class);

        assertTrue(result.getName().isPresent());
        assertEquals("outer name", result.getName().getValue());

        assertFalse(result.getNote().isPresent());

        String serialized = testObjectMapper().writeValueAsString(result);

        // The serializer should NOT output "note"
        assertTrue(serialized.contains("\"name\":\"outer name\""));
        assertFalse(serialized.contains("note"));
    }

    @Test
    public void shouldDeserializeAndSerializeJsonPropertyOnField() {

        String json = """
                    {
                        "name": "value 1"
                    }
                """;

        TestJson2Req result = testObjectMapper().readValue(json, TestJson2Req.class);
        assertEquals("name", result.getName().getFieldName());

        assertTrue(result.getName().isPresent());
        assertEquals("value 1", result.getName().getValue());
        assertEquals("name", result.getName().getFieldName());
        assertFalse(result.getNote().isPresent());

        String serialized = testObjectMapper().writeValueAsString(result);

        assertTrue(serialized.contains("\"name_1\":\"value 1\""));
        assertFalse(serialized.contains("\"name\""));
    }

    @Test
    public void toMapShouldContainOnlyPresentFields() {
        String json = """
                    {
                        "name": "outer name"
                    }
                """;

        TestJsonReq result = testObjectMapper().readValue(json, TestJsonReq.class);
        Map<String, Object> map = result.toMap();

        assertEquals(1, map.size());
        assertEquals("outer name", map.get("name"));
        assertFalse(map.containsKey("note"));
    }

    @Test
    public void toMapShouldContainAllPresentFields() {
        String json = """
                    {
                        "name": "outer name",
                        "note": "a note"
                    }
                """;

        TestJsonReq result = testObjectMapper().readValue(json, TestJsonReq.class);
        Map<String, Object> map = result.toMap();

        assertEquals(2, map.size());
        assertEquals("outer name", map.get("name"));
        assertEquals("a note", map.get("note"));
    }

    @Test
    public void toMapShouldIncludePresentNullValue() {
        String json = """
                    {
                        "name": null
                    }
                """;

        TestJsonReq result = testObjectMapper().readValue(json, TestJsonReq.class);
        Map<String, Object> map = result.toMap();

        assertEquals(1, map.size());
        assertTrue(map.containsKey("name"));
        assertNull(map.get("name"));
        assertFalse(map.containsKey("note"));
    }

    @Test
    public void toMapShouldReturnEmptyMapWhenNoFieldsPresent() {
        TestJsonReq result = new TestJsonReq();
        Map<String, Object> map = result.toMap();

        assertTrue(map.isEmpty());
    }

    @Test
    public void toMapShouldUseJavaFieldNameNotJsonPropertyName() {
        String json = """
                    {
                        "name": "value 1"
                    }
                """;

        TestJson2Req result = testObjectMapper().readValue(json, TestJson2Req.class);
        Map<String, Object> map = result.toMap();

        assertEquals(1, map.size());
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("name_1"));
        assertEquals("value 1", map.get("name"));
    }


}
