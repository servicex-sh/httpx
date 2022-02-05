package com.alibaba.fastjson;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mvnsearch.http.utils.JsonUtils.OBJECT_MAPPER;


public class JSON {

    public static String toJSONString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String toJSONString(Object obj, boolean prettyFormat) {
        try {
            if (prettyFormat) {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            } else {
                return OBJECT_MAPPER.writeValueAsString(obj);
            }
        } catch (Exception ignore) {
            return "{}";
        }
    }

    public static Object parse(String text) {
        try {
            if (text.startsWith("[")) {
                return OBJECT_MAPPER.readValue(text, List.class);
            } else if (text.startsWith("{")) {
                return OBJECT_MAPPER.readValue(text, Map.class);
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
            return OBJECT_MAPPER.readValue(text, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        try {
            JsonNode tree = OBJECT_MAPPER.readTree(text);
            List<T> list = new ArrayList<T>();
            for (JsonNode jsonNode : tree) {
                list.add(OBJECT_MAPPER.treeToValue(jsonNode, clazz));
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
