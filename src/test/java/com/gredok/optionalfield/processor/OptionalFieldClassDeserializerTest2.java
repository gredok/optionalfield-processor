package com.gredok.optionalfield.processor;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
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
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
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

    @OptionalClassReq
    public static class TestJsonWithList {
        private List<Test22> items;
    }

    @OptionalClassReq
    public static class TestJsonWithConstant {
        public static final String DEFAULT_NAME = "default";
        private String name;
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
                        "name_1": "value 1"
                    }
                """;

        TestJson2Req result = testObjectMapper().readValue(json, TestJson2Req.class);
        assertEquals("name_1", result.getName().getFieldName());

        assertTrue(result.getName().isPresent());
        assertEquals("value 1", result.getName().getValue());
        assertEquals("name_1", result.getName().getFieldName());
        assertFalse(result.getNote().isPresent());

        String serialized = testObjectMapper().writeValueAsString(result);

        assertTrue(serialized.contains("\"name_1\":\"value 1\""));
        assertFalse(serialized.contains("\"name\""));
    }

    @Test
    public void shouldTreatRawFieldNameAsUnknownWhenJsonPropertyAliasDiffers() {
        String json = """
                    {
                        "name": "value 1"
                    }
                """;

        // "name" is the Java field name, but TestJson2.name is annotated @JsonProperty("name_1"),
        // so the raw field name is no longer a recognized JSON key.
        assertThrows(Exception.class, () -> testObjectMapper().readValue(json, TestJson2Req.class));
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
                        "name_1": "value 1"
                    }
                """;

        TestJson2Req result = testObjectMapper().readValue(json, TestJson2Req.class);
        Map<String, Object> map = result.toMap();

        assertEquals(1, map.size());
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("name_1"));
        assertEquals("value 1", map.get("name"));
    }

    @Test
    public void shouldPreserveEmptyStringInsteadOfCoercingToNull() {
        String json = """
                    {
                        "name": ""
                    }
                """;

        TestJsonReq result = testObjectMapper().readValue(json, TestJsonReq.class);

        assertTrue(result.getName().isPresent());
        assertEquals("", result.getName().getValue());
    }

    @Test
    public void settingFieldToNullShouldNotBreakToMap() {
        TestJsonReq req = new TestJsonReq();
        req.setName(null);

        assertFalse(req.getName().isPresent());

        Map<String, Object> map = req.toMap();

        assertTrue(map.isEmpty());
    }

    @Test
    public void shouldFailOnUnknownPropertyWhenFeatureEnabled() {
        String json = """
                    {
                        "name": "outer name",
                        "unexpected": "surprise"
                    }
                """;

        // FAIL_ON_UNKNOWN_PROPERTIES is enabled by default.
        assertThrows(Exception.class, () -> testObjectMapper().readValue(json, TestJsonReq.class));
    }

    @Test
    public void shouldIgnoreUnknownPropertyWhenFeatureDisabled() {
        String json = """
                    {
                        "name": "outer name",
                        "unexpected": "surprise"
                    }
                """;

        ObjectMapper lenientMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.USE_ANNOTATIONS)
                .annotationIntrospector(new JacksonAnnotationIntrospector())
                .findAndAddModules()
                .build();

        TestJsonReq result = lenientMapper.readValue(json, TestJsonReq.class);

        assertTrue(result.getName().isPresent());
        assertEquals("outer name", result.getName().getValue());
    }

    @Test
    public void shouldDeserializeCollectionElementsAsDtosNotMaps() {
        String json = """
                    {
                        "items": [
                            {"name": "a", "note": "b"}
                        ]
                    }
                """;

        TestJsonWithListReq result = testObjectMapper().readValue(json, TestJsonWithListReq.class);

        assertTrue(result.getItems().isPresent());
        List<Test22> items = result.getItems().getValue();
        assertEquals(1, items.size());
        assertInstanceOf(Test22.class, items.get(0));
        assertEquals("a", items.get(0).getName());
        assertEquals("b", items.get(0).getNote());
    }

    @Test
    public void shouldNotIncludeStaticFieldsInGeneratedClass() {
        List<String> fieldNames = Arrays.stream(TestJsonWithConstantReq.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertFalse(fieldNames.contains("DEFAULT_NAME"));
        assertTrue(fieldNames.contains("name"));
    }

}
