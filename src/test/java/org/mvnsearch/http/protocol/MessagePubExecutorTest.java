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
    public void testSendRocketMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send rabbit message
                PUB queue1
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
    public void testSendAliyunEventBridgeMessage() throws Exception {
        Map<String, Object> context = new HashMap<>();
        @Language("HTTP Request")
        String httpFile = """
                ### send aliyun event bridge message
                //@name ali-event
                PUB demo-event-bus
                Host: eventbridge://endpoint_host
                Authorization: Basic your_key_id:your_key_secret
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
