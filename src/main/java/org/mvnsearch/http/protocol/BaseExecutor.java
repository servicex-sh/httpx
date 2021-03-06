package org.mvnsearch.http.protocol;

import com.jayway.jsonpath.JsonPath;
import org.jetbrains.annotations.Nullable;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.utils.JsonUtils;
import org.mvnsearch.http.vendor.Nodejs;
import picocli.CommandLine;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
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
            if (jsonText.startsWith("{")) {
                final Map<?, ?> jsonObject = JsonUtils.readValue(jsonText, Map.class);
                return JsonUtils.writeValueAsPrettyColorString(jsonObject);
            } else if (jsonText.startsWith("[")) {
                final List<?> jsonArray = JsonUtils.readValue(jsonText, List.class);
                return JsonUtils.writeValueAsPrettyColorString(jsonArray);
            } else {
                return jsonText;
            }
        } catch (Exception e) {
            return jsonText;
        }
    }

    default String prettyJsonFormatWithJsonPath(String jsonText, @Nullable String jsonPath) {
        if (jsonPath == null) {
            return prettyJsonFormat(jsonText);
        }
        try {
            if (jsonText.startsWith("{") || jsonText.startsWith("[")) {
                final Object result = JsonPath.read(jsonText, jsonPath);
                if (result != null) {
                    return JsonUtils.writeValueAsPrettyString(result);
                } else {
                    return "{}";
                }
            } else {
                return jsonText;
            }
        } catch (Exception e) {
            return jsonText;
        }
    }

    default Map<String, String> queryToMap(URI url) {
        String queryPart = url.getQuery();
        if (queryPart == null || queryPart.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> queryParams = new HashMap<>();
        try {
            String[] pairs = queryPart.split("&");
            for (String pair : pairs) {
                String[] keyValuePair = pair.split("=");
                queryParams.put(URLDecoder.decode(keyValuePair[0], StandardCharsets.UTF_8.name()),
                        URLDecoder.decode(keyValuePair[1], StandardCharsets.UTF_8.name()));
            }
        } catch (Exception ignore) {

        }
        return queryParams;
    }

    default String colorOutput(String color, String text) {
        return CommandLine.Help.Ansi.AUTO.string("@|" + color + " " + text + " |@");
    }


    default void runJsTest(HttpRequest httpRequest, int statusCode, Map<String, String> headers, String contentType, String body) {
        final String javaScriptTestCode = httpRequest.getJavaScriptTestCode();
        if (javaScriptTestCode != null && !javaScriptTestCode.isEmpty()) {
            System.out.println();
            System.out.println("============Execute JS Test============");
            final String jsTestOutput = Nodejs.executeHttpClientCode(javaScriptTestCode, statusCode, headers, contentType, body);
            System.out.println(jsTestOutput);
        }
    }
}
