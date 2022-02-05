package com.alibaba.fastjson;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class JSONObject extends HashMap<String, Object> {

    public String getString(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public JSONArray getJSONArray(String key) {
        Object value = this.get(key);

        if (value instanceof JSONArray) {
            return (JSONArray) value;
        }

        if (value instanceof List) {
            return new JSONArray((List<?>) value);
        }

        if (value instanceof String) {
            return new JSONArray((List<?>) JSON.parse((String) value));
        }

        return new JSONArray(Collections.singletonList(value));
    }
}
