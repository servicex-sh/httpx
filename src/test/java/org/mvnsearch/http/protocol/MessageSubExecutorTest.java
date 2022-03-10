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
                URI: kafka://localhost:9092/
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
                URI: amqp://localhost:5672
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }

    @Test
    public void testSubscribeStomp() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### subscribe rabbitmq
                SUB queue3
                URI: stomp://localhost:61613
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
                URI: mqtt://localhost:1883
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
                URI: nats://localhost:4222
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }

    @Test
    public void testSubscribeZeroMQ() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### subscribe zeromq
                SUB topic1
                URI: zeromq://localhost:5555
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
                URI: redis://localhost:6379
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }

    @Test
    public void testSubscribePulsar() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### subscribe pulsar
                SUB test-topic
                URI: pulsar://localhost:6650
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
                URI: rocketmq://localhost:9876
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessageSubscribeExecutor().execute(request);
    }
}
