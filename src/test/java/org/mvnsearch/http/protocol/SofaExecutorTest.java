package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class SofaExecutorTest {
    @Test
    public void testHttpRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### dubbo hi
                SOFA 127.0.0.1:12201/org.mvnsearch.HelloService/findById(java.lang.Integer)
                Content-Type: application/json
                     
                1  
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new SofaRpcExecutor().execute(request);
    }
}
