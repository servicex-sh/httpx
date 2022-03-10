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
                URI: kafka://localhost:9092/
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
                URI: amqp://localhost:5672
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
                URI: stomp://localhost:61613
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
                URI: rocketmq://localhost:9876
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
                URI: nats://localhost:4222
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
                URI: pulsar://localhost:6650
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
                URI: mqtt://localhost:1883
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
                URI: redis://localhost:6379
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
    public void testSendSNSMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send sns message
                PUB sns-demo
                URI: arn:aws:sns:us-east-1:632793027037:sns-demo
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
    public void testSendAwsBridgeEventMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send aws eventbridge message
                PUB eventbus-demo
                URI: arn:aws:events:us-east-1:632793027037:event-bus/eventbus-demo
                Content-Type: application/json
                                                
               {
                 "specversion": "1.0",
                 "source": "demo.event",
                 "type": "com.example.someevent",
                 "datacontenttype": "application/json",
                 "data": {
                   "name": "jackie"
                 }
               }
                """;
        HttpRequest request = HttpRequestParser.parse(httpFile, context).get(0);
        request.cleanBody();
        new MessagePublishExecutor().execute(request);
    }

}
