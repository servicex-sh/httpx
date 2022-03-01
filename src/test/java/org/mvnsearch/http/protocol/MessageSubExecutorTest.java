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
                SUB testTopic
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
                SUB queue3
                Host: amqp://localhost:5672
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }

    @Test
    public void testSubscribeMqtt() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### subscribe mqtt
                SUB topic1
                Host: mqtt://localhost:1883
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
                SUB subject1
                Host: nats://localhost:4222
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }

    @Test
    public void testSubscribeRedis() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### subscribe redis
                SUB channel1
                Host: redis://localhost:6379
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }

    @Test
    public void testSubscribeRocketmq() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### subscribe rabbitmq
                SUB testTopic
                Host: rocketmq://localhost:9876
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }
}
