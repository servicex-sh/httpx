package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

public class JsonRestExecutorTest {

    @Test
    public void testHttpRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello post
                REST https://httpbin.org/post
                X-Args-0: 1
                Content-Type: text/html
                        
                hello
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new JsonRestExecutor().execute(request);
    }

    @Test
    public void testHttpRequestObject() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello post
                REST https://httpbin.org/post
                X-Args-id: 1
                X-Body-Name: intro
                Content-Type: text/html
                        
                <div id="xxx">
                  hello
                </div>
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new JsonRestExecutor().execute(request);
    }
}
