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
                ### ChatGPT with JBang
                CHATGPT https://api.openai.com/v1/chat/completions
                        
                You are to generate Java code in the style of jbang, and main class must be named Hello. {.system}
                                
                Build a CLI app with Picocli 4.7.3 library, and include name and email options. Do not add any additional text.
                                
                Please use Java 17. {.assistant}
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new ChatGPTExecutor().execute(request);
    }

}
