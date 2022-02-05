package com.alibaba.fastjson;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class JSONArray extends ArrayList<Object> {
    public JSONArray() {
    }

    public JSONArray(Collection<?> c) {
        super(c);
    }

    public String getString(int i) {
        return get(i).toString();
    }

    public <T> List<T> toJavaList(Class<T> clazz) {
        if (this.isEmpty()) {
            return Collections.emptyList();
        } else {
            return JSON.parseArray(JSON.toJSONString(this), clazz);
        }
    }
}
