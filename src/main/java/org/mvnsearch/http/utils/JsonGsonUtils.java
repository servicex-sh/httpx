package org.mvnsearch.http.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class JsonGsonUtils {
    public static final Gson gson = new Gson();
    public static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

    public static String writeValueAsString(Object obj) {
        try {
            return gson.toJson(obj);
        } catch (Exception e) {
            return "";
        }
    }

    public static byte[] writeValueAsBytes(Object obj) {
        try {
            return gson.toJson(obj).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new byte[]{};
        }
    }

    public static <T> T readValue(String jsonText, Class<T> valueType) throws IOException {
        return gson.fromJson(jsonText, valueType);
    }

    public static <T> T readValue(byte[] jsonBytes, Class<T> valueType) throws IOException {
        return gson.fromJson(new String(jsonBytes, StandardCharsets.UTF_8), valueType);
    }

    public static <T> T readValue(File jsonFile, Class<T> valueType) throws IOException {
        return gson.fromJson(new FileReader(jsonFile), valueType);
    }

    public static <T> List<T> readArray(String text, Class<T> clazz) {
        try {
            List<T> list = new ArrayList<T>();
            JsonArray jsonArray = gson.fromJson(text, JsonArray.class);
            for (JsonElement jsonElement : jsonArray) {
                list.add(gson.fromJson(jsonElement, clazz));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static String writeValueAsPrettyString(Object obj) {
        return prettyGson.toJson(obj);
    }
}

