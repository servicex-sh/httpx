package org.mvnsearch.http.logging;

import org.slf4j.LoggerFactory;

public class HttpxErrorCodeLoggerFactory {
    public static HttpxErrorCodeLogger getLogger(Class<?> clazz) {
        return new HttpxErrorCodeLogger(LoggerFactory.getLogger(clazz));
    }

    public static HttpxErrorCodeLogger getLogger(String name) {
        return new HttpxErrorCodeLogger(LoggerFactory.getLogger(name));
    }

}
