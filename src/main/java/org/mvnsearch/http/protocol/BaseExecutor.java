package org.mvnsearch.http.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mvnsearch.http.model.HttpRequest;

import java.util.List;
import java.util.Map;

public interface BaseExecutor {

    List<byte[]> execute(HttpRequest httpRequest);


    default boolean isPrintable(String contentType) {
        return contentType.startsWith("text/")
                || contentType.contains("javascript")
                || contentType.contains("typescript")
                || contentType.contains("ecmascript")
                || contentType.contains("json")
                || contentType.contains("xml")
                || contentType.contains("yaml");
    }

    default String prettyJsonFormat(String jsonText) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            if (jsonText.startsWith("{")) {
                final Map<?, ?> jsonObject = objectMapper.readValue(jsonText, Map.class);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            } else if (jsonText.startsWith("[")) {
                final List<?> jsonArray = objectMapper.readValue(jsonText, List.class);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonArray);
            } else {
                return jsonText;
            }
        } catch (Exception e) {
            return jsonText;
        }
    }
}
