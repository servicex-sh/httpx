package org.mvnsearch.http.protocol;

import org.mvnsearch.http.model.HttpRequest;

import java.util.List;

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
}
