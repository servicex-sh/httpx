package org.mvnsearch.http.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class JsonUtils {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static String writeValueAsString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "";
        }
    }

    public static byte[] writeValueAsBytes(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (Exception e) {
            return new byte[]{};
        }
    }


    public static <T> T readValue(String jsonText, Class<T> valueType) throws IOException {
        return OBJECT_MAPPER.readValue(jsonText, valueType);
    }

    public static <T> T readValue(byte[] jsonBytes, Class<T> valueType) throws IOException {
        return OBJECT_MAPPER.readValue(jsonBytes, valueType);
    }

    public static <T> T readValue(File jsonFile, Class<T> valueType) throws IOException {
        return OBJECT_MAPPER.readValue(jsonFile, valueType);
    }

    public static <T> List<T> readArray(String text, Class<T> clazz) {
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


    public static String writeValueAsPrettyString(Object obj) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return "";
        }
    }

    public static String writeValueAsPrettyColorString(Object obj) {
        if (!CommandLine.Help.Ansi.AUTO.enabled()) {
            return writeValueAsPrettyString(obj);
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper(new JsonColorFactory());
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "";
        }
    }
}

