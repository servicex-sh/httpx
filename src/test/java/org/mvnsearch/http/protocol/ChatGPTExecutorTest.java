package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

public class ChatGPTExecutorTest {

    @Test
    public void testHttpRequest() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### hello post
                CHATGPT https://api.openai.com/v1/chat/completions
                Content-Type: text/markdown
                        
                What's Java?
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new ChatGPTExecutor().execute(request);
    }

}
