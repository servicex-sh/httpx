package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpExecutorTest {

    @Test
    public void testHttpRequestMultiLines() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello ip
                GET https://httpbin.org/get
                  ?id=1
                  &name=xxx
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new HttpExecutor().execute(request);
    }

    @Test
    public void testHttpRequestGetIp() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello ip
                GET https://httpbin.org/ip
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new HttpExecutor().execute(request);
    }

    @Test
    public void testHttpRequestGetIpWithJSTest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello ip
                GET https://httpbin.org/ip
                                
                > {%
                    client.test("Request executed successfully", function() {
                        client.log(response.status);
                        client.log(response.contentType);
                        client.log(response.body);
                    });
                %}
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new HttpExecutor().execute(request);
    }

    @Test
    public void testHttpRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello post
                POST https://httpbin.org/post
                Content-Type: application/json
                        
                > {%
                client.log("first");
                %}
                >>! demo.json
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        assertThat(request.getJavaScriptTestCode()).contains("first");
        assertThat(request.getRedirectResponse()).isEqualTo(">>! demo.json");
        new HttpExecutor().execute(request);
    }
}
