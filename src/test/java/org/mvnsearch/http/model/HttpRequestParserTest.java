package org.mvnsearch.http.model;

import io.netty.buffer.ByteBuf;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class HttpRequestParserTest {

    @Test
    public void testEvaluateTemplate() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "linux_china");
        context.put("age", "40");
        String template = "%{age}";
        System.out.println(HttpRequestParser.evaluateTemplate(template, context));
    }

    @Test
    public void testEvaluateFunction() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "linux_china");
        context.put("age", "40");
        System.out.println(HttpRequestParser.evaluateFunction("$randomInt", context));
        System.out.println(HttpRequestParser.evaluateFunction("$randomInt 1 `%{age}`", context));
        System.out.println(HttpRequestParser.evaluateFunction("$base64 'abc 1234'", context));
    }

    @Test
    public void testReplaceVariables() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("base-url", "https://httpbin.org");
        context.put("token", "xxx.yyy.zzz");
        @Language("HTTP Request")
        String httpFile = """
                ### hello post
                POST {{base-url}}/post
                Content-Type: application/json
                Authorization: bear {{token}}

                {
                  "id": 1
                }
                """;
        final String newHttpFile = HttpRequestParser.replaceVariables(httpFile, context);
        System.out.println(newHttpFile);
    }

    @Test
    public void testJavaScriptTestCode() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                @host = https://httpbin.org
                
                ### hello post
                POST {{host}}/post
                Content-Type: application/json

                {
                  "id": 1
                }
                                
                > {%
                    client.test("Request executed successfully", function() {
                        client.assert(response.status === 200, "Response status is not 200");
                    });
                %}
                """;
        List<HttpRequest> requests = HttpRequestParser.parse(httpFile, context);
        assertThat(requests).isNotEmpty();
        HttpRequest request = requests.get(0);
        request.cleanBody();
        System.out.println("========JavaScript Code===============");
        System.out.println(request.getJavaScriptTestCode());
        System.out.println("======================================");
        assertThat(request.getJavaScriptTestCode()).isNotEmpty();
        final ByteBuf bodyBuf = request.requestBody().block();
        assert bodyBuf != null;
        final String body = bodyBuf.toString(StandardCharsets.UTF_8);
        assertThat(body).isEqualTo("""
                {
                  "id": 1
                }""");
    }

    @Test
    public void testSimpleHttpRequest() {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = "GET https://httpbin.org/ip?id={{$uuid}}";
        List<HttpRequest> requests = HttpRequestParser.parse(httpFile, context);
        assertThat(requests).isNotEmpty();
        HttpRequest request = requests.get(0);
        assertThat(request.getMethod().getName()).isEqualTo("GET");
    }

    @Test
    public void testMultiRequestParse() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("base-url", "https://httpbin.org");
        @Language("HTTP Request")
        String httpFile = """
                #!/usr/bin/env
                ### hello post
                # @name post
                POST {{base-url}}/post
                Content-Type: application/json
                Authorization: bear {{token}}
                       
                {
                  "id": 1
                }
                                
                ### second get
                                
                                
                GET {{base-url}}/ip
                Host: localhost
                              
                """;
        List<HttpRequest> requests = HttpRequestParser.parse(httpFile, context);
        assertThat(requests).isNotEmpty();
        HttpRequest request = requests.get(0);
        request.cleanBody();
        assertThat(request.getName()).isEqualTo("post");
        assertThat(request.getMethod().getName()).isEqualTo("POST");
    }

    @Test
    public void testPostParse() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("base-url", "https://httpbin.org");
        context.put("token", "xxx.yyy.zzz");
        @Language("HTTP Request")
        String httpFile = """
                ### hello post
                POST {{base-url}}/post
                Content-Type: application/json
                Authorization: bear {{token}}

                {
                  "id": 1
                }
                                
                """;
        List<HttpRequest> httpRequests = HttpRequestParser.parse(httpFile, context);
        assertThat(httpRequests).isNotEmpty();
        HttpRequest httpRequest = httpRequests.get(0);
        httpRequest.cleanBody();
        assertThat(httpRequest.getMethod().getName()).isEqualTo("POST");
        assertThat(httpRequest.getHeader("Authorization")).isEqualTo("bear xxx.yyy.zzz");
        assertThat(new String(httpRequest.getBodyBytes())).isEqualTo("""
                {
                  "id": 1
                }""");
    }

}
