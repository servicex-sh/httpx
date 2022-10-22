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
                GET https://httpbin.org/get?
                  id=1&
                  name=xxx
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new HttpExecutor().execute(request);
    }

    @Test
    public void testHttpRequestPreCode() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello pre script
                < {%
                  request.variables.set("name","linux_china");
                  request.variables.set("name2","name2");
                %}
                #@name demo
                GET https://httpbin.org/get?id=1&name={{name}}
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new HttpExecutor().execute(request);
    }

    @Test
    public void testHttpStatusCode() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello status
                POST https://httpbin.org/status/401
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new HttpExecutor().execute(request);
    }

    @Test
    public void testDownloadFile() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("logo", "https://httpbin.org/ip");
        @Language("HTTP Request")
        String httpFile = """
                ### hello
                GET {{logo}}
                                
                >>! temp/demo.txt
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
    public void testHttpPostWithFormUrlencoded() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello ip
                POST https://httpbin.org/post
                Content-Type: application/x-www-form-urlencoded
                                
                foo1=bar
                &foo2=bar
                &foo3=bar
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new HttpExecutor().execute(request);
    }

    @Test
    public void testHttpPostWithRandom() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello post
                POST https://httpbin.org/post
                Content-Type: application/x-www-form-urlencoded
                                
                uuid={{$random.hexadecimal}}
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
