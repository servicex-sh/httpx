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
                SUB topic-1
                Host: kafka://localhost:9092/
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }

    @Test
    public void testSubscribeRabbit() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### subscribe rabbitmq
                SUB queue1
                Host: amqp://guest:guest@localhost:5672
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }

    @Test
    public void testSubscribeNats() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### subscribe rabbitmq
                SUB queue1
                Host: nats://localhost:4222
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }
}
