package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class DubboExecutorTest {
    @Test
    public void testHttpRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### dubbo hi
                DUBBO 127.0.0.1:20880/GreetingsService/sayHi(java.lang.String)
                Content-Type: application/json
                     
                "Jackie"     
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new DubboExecutor().execute(request);
    }

    @Test
    public void testHttpRequestWithArgs() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### dubbo hi
                DUBBO 127.0.0.1:20880/org.mvnsearch.UserService/updateDesc(java.lang.Integer,java.lang.String)
                Content-Type: text/html
                X-Args-0: 1
                                
                <div id="xxx">xxf sdfx</div>
                gooodle    
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new DubboExecutor().execute(request);
    }
}
