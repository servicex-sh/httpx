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
                        
                You are to generate Kotlin code in the style of jbang, and main class must be named Hello.
                Do not add any additional text. {.system}
                
                Build a CLI app with Picocli 4.7.3 library, and include name and email options.
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new ChatGPTExecutor().execute(request);
    }

}
