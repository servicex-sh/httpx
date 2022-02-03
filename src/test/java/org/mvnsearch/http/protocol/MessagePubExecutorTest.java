package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class MessagePubExecutorTest {
    @Test
    public void testSendMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send kafka message
                //@name kafka
                PUB kafka://localhost:9092/topic-1
                Content-Type: application/json
                               
                {
                  "name": "Jackie"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessagePublishExecutor().execute(request);
    }
}
