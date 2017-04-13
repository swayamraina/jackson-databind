package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class TestNullHandling extends BaseMapTest
{
    static class FunnyNullDeserializer extends JsonDeserializer<String>
    {
        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            return "text";
        }

        @Override
        public String getNullValue() { return "funny"; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // Test for [JACKSON-643]
    public void testCustomRootNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        // should get non-default null directly:
        String str = mapper.readValue("null", String.class);
        assertNotNull(str);
        assertEquals("funny", str);
        
        // as well as via ObjectReader
        ObjectReader reader = mapper.reader(String.class);
        str = reader.readValue("null");
        assertNotNull(str);
        assertEquals("funny", str);
    }

    // Test for [#407]
    public void testListOfNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        List<String> list = Arrays.asList("funny");
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, String.class);

        // should get non-default null directly:
        List<?> deser = mapper.readValue("[null]", type);
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals(list.get(0), deser.get(0));

        // as well as via ObjectReader
        ObjectReader reader = mapper.reader(type);
        deser = reader.readValue("[null]");
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals(list.get(0), deser.get(0));
    }

    // Test for [#407]
    public void testMapOfNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        JavaType type = mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class);
        // should get non-default null directly:
        Map<?,?> deser = mapper.readValue("{\"key\":null}", type);
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals("funny", deser.get("key"));

        // as well as via ObjectReader
        ObjectReader reader = mapper.reader(type);
        deser = reader.readValue("{\"key\":null}");
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals("funny", deser.get("key"));
    }

    static class RootData {
        public String name;
        public String type;
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type")
        @JsonSubTypes({
                @Type(value = TypeA.class, name = "TypeA"),
                @Type(value = TypeB.class, name = "TypeB")})
        public Proxy proxy;

        public RootData() {}

        public RootData(String name, String type, Proxy proxy) {
            this.name = name;
            this.type = type;
            this.proxy = proxy;
        }
    }

    static interface Proxy {

    }

    static class TypeA implements Proxy {
        public String aValue;

        public TypeA() {}

        public TypeA(String a) {
            this.aValue = a;
        }
    }

    static class TypeB implements Proxy {
        public String bValue;

        public TypeB() {}

        public TypeB(String b) {
            this.bValue = b;
        }
    }

    public void testPolymorphicDataNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String typeA =
                "{\"name\":\"TypeAData\", \"type\":\"TypeA\", \"proxy\":{\"aValue\":\"This works!\"}}";
        RootData typeAData = mapper.readValue(typeA, RootData.class);
        assertEquals("No value for aValue!?", "This works!", ((TypeA) typeAData.proxy).aValue);
        String typeB =
                "{\"name\":\"TypeBData\", \"type\":\"TypeB\", \"proxy\":{\"bValue\":\"This works too!\"}}";
        RootData typeBData = mapper.readValue(typeB, RootData.class);
        assertEquals("No value for bValue!?", "This works too!", ((TypeB) typeBData.proxy).bValue);
        String typeBNull =
                "{\"name\":\"TypeBData\", \"type\":\"TypeB\", \"proxy\": null}";
        RootData typeBNullData = mapper.readValue(typeBNull, RootData.class);
        assertNull("Proxy should be null!", typeBNullData.proxy);
    }
}