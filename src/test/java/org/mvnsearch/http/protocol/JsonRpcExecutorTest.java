package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class JsonRpcExecutorTest {
    @Test
    public void testReqResp() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### jsonrpc request
                JSONRPC http://127.0.0.1:8080/add
                Content-Type: application/json
                     
                [1, 2]  
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new JsonRpcExecutor().execute(request);
    }

    @Test
    public void testTcpRequestResponse() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### jsonrpc request
                JSONRPC 127.0.0.1:9080/add
                Content-Type: application/json
                     
                [1, 2]  
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new JsonRpcExecutor().execute(request);
    }

}
