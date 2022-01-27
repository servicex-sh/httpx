package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

public class HttpExecutorTest {
    @Test
    public void testHttpRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello post
                POST https://httpbin.org/post
                Content-Type: application/json
                        
                {
                  "id": 1
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        new HttpExecutor().execute(request);
    }
}
