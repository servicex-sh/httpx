package com.alibaba.fastjson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class JSON {
    public static final ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public static String toJSONString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String toJSONString(Object obj, boolean prettyFormat) {
        try {
            if (prettyFormat) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            } else {
                return objectMapper.writeValueAsString(obj);
            }
        } catch (Exception ignore) {
            return "{}";
        }
    }

    public static Object parse(String text) {
        try {
            if (text.startsWith("[")) {
                return objectMapper.readValue(text, List.class);
            } else if (text.startsWith("{")) {
                return objectMapper.readValue(text, Map.class);
            } else {
                return text;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return text;
        }
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        try {
            return objectMapper.readValue(text, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        try {
            JsonNode tree = objectMapper.readTree(text);
            List<T> list = new ArrayList<T>();
            for (JsonNode jsonNode : tree) {
                list.add(objectMapper.treeToValue(jsonNode, clazz));
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
