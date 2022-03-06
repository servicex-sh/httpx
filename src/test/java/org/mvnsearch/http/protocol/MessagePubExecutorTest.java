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
                PUB testTopic
                Host: kafka://localhost:9092/
                Content-Type: application/json
                               
                {
                  "name": "Jacki!!!e"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessagePublishExecutor().execute(request);
    }

    @Test
    public void testSendRabbitMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send rabbit message
                PUB queue3
                Host: amqp://localhost:5672
                Content-Type: application/json
                               
                {
                  "name": "Jackie"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessagePublishExecutor().execute(request);
    }

    @Test
    public void testSendStompMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send stomp message
                PUB queue3
                Host: stomp://localhost:61613
                Content-Type: application/json
                               
                {
                  "name": "Jackie"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessagePublishExecutor().execute(request);
    }

    @Test
    public void testSendRocketMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send rocketmq message
                PUB testTopic
                Host: rocketmq://localhost:9876
                Content-Type: application/json
                               
                {
                  "name": "Jackie"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessagePublishExecutor().execute(request);
    }

    @Test
    public void testSendNatsMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send rabbit message
                PUB subject1
                Host: nats://localhost:4222
                Content-Type: application/json
                               
                {
                  "name": "Jackie"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessagePublishExecutor().execute(request);
    }

    @Test
    public void testSendPulsarMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send pulsar message
                PUB test-topic
                Host: pulsar://localhost:6650
                Content-Type: application/json
                               
                {
                  "name": "Jackie"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessagePublishExecutor().execute(request);
    }

    @Test
    public void testSendMqttMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send mqtt message
                PUB topic1
                Host: mqtt://localhost:1883
                Content-Type: application/json
                               
                {
                  "name": "Jackie"
                }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessagePublishExecutor().execute(request);
    }

    @Test
    public void testSendRedisMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send redis message
                PUB channel1
                Host: redis://localhost:6379
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
