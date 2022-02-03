package org.mvnsearch.http.protocol;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.model.HttpRequestParser;

import java.util.HashMap;
import java.util.Map;

@Disabled
public class MessageSubExecutorTest {
    @Test
    public void testSubscribe() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send kafka message
                //@name kafka
                SUB topic:kafka://localhost:9092/topic-1
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }
}
