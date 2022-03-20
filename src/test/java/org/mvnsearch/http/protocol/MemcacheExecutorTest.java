package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class MemcacheExecutorTest {
    @Test
    public void testMemcacheSet() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### memcache set
                MEMCACHE nick
                Host: localhost:11211
                Content-Type: text/plain
                     
                Jackie    
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MemcacheExecutor().execute(request);
    }

    @Test
    public void testMemcacheGet() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### memcache get
                MEMCACHE nick
                Host: localhost:11211
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MemcacheExecutor().execute(request);
    }
}
