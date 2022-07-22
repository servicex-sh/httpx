package com.alibaba.fastjson;

import com.alibaba.fastjson.serializer.SerializerFeature;
import org.mvnsearch.http.utils.JsonUtils;

import java.util.List;
import java.util.Map;


@SuppressWarnings("unused")
public class JSON {

    public static String toJSONString(Object obj) {
        try {
            return JsonUtils.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String toJSONString(Object obj, boolean prettyFormat) {
        try {
            if (prettyFormat) {
                return JsonUtils.writeValueAsPrettyString(obj);
            } else {
                return JsonUtils.writeValueAsString(obj);
            }
        } catch (Exception ignore) {
            return "{}";
        }
    }

    public static String toJSONString(Object obj, SerializerFeature... features) {
        try {
            return JsonUtils.writeValueAsString(obj);
        } catch (Exception ignore) {
            return "{}";
        }
    }

    public static Object parse(String text) {
        try {
            if (text.startsWith("[")) {
                return JsonUtils.readValue(text, List.class);
            } else if (text.startsWith("{")) {
                return JsonUtils.readValue(text, Map.class);
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
            return JsonUtils.readValue(text, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        return JsonUtils.readArray(text, clazz);
    }
}
