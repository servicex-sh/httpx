package org.mvnsearch.http.vendor;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class NodejsTest {
    @Test
    public void testExecuteJsBlock() throws Exception {
        @Language("JavaScript")
        String jsCode = """
                client.test("Request executed successfully", function () {
                    client.log(response.status)
                    client.log(response.contentType)
                    client.log(response.body)
                });
                """;
        int statusCode = 200;
        String contentType = "text/plain";
        String body = "hello world";
        Map<String, String> headers = new HashMap<>();
        String result = Nodejs.executeHttpClientCode(jsCode, statusCode, headers, contentType, body);
        System.out.println(result);
    }

    @Test
    public void testExecutePreScript() throws Exception {
        @Language("JavaScript")
        String jsCode = """
                request.variables.set("name","jackie");
                request.variables.set("age","2");
                """;
        String result = Nodejs.executePreScriptCode(jsCode);
        System.out.println(result);
    }
}
