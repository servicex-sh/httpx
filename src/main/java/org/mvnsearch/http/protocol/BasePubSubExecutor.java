package org.mvnsearch.http.protocol;

import org.mvnsearch.http.model.HttpRequest;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.net.URI;
import java.util.concurrent.CountDownLatch;


public interface BasePubSubExecutor extends BaseExecutor {

    record UriAndSubject(String uri, String subject) {
    }

    default UriAndSubject getRedisUriAndChannel(URI redisURI, HttpRequest httpRequest) {
        String connectionUri;
        String channel;
        final String hostHeader = httpRequest.getHeader("Host");
        if (hostHeader != null) {
            connectionUri = hostHeader;
            channel = httpRequest.getRequestTarget().getRequestLine();
        } else {
            connectionUri = redisURI.toString();
            final int offset = connectionUri.lastIndexOf("/");
            channel = connectionUri.substring(offset + 1);
            connectionUri = connectionUri.substring(0, offset);
        }
        return new UriAndSubject(connectionUri, channel);
    }

    default UriAndSubject getRabbitUriAndQueue(URI rabbitURI, HttpRequest httpRequest) {
        String connectionUri;
        String queue;
        final String hostHeader = httpRequest.getHeader("Host");
        if (hostHeader != null) {
            connectionUri = hostHeader;
            queue = httpRequest.getRequestTarget().getRequestLine();
        } else {
            connectionUri = rabbitURI.toString();
            queue = queryToMap(rabbitURI).get("queue");
        }
        return new UriAndSubject(connectionUri, queue);
    }

    default UriAndSubject getMqttUriAndTopic(URI mqttURI, HttpRequest httpRequest) {
        String topic = mqttURI.getPath().substring(1);
        String schema = mqttURI.getScheme();
        String brokerUrl = mqttURI.toString();
        if (schema.contains("+")) {
            brokerUrl = brokerUrl.substring(brokerUrl.indexOf("+") + 1);
        } else {
            brokerUrl = brokerUrl.replace("mqtt://", "tcp://");
        }
        brokerUrl = brokerUrl.substring(0, brokerUrl.lastIndexOf("/"));
        return new UriAndSubject(brokerUrl, topic);
    }

    default StompHeaders constructStompHeaders(URI stompURI, HttpRequest httpRequest) {
        StompHeaders headers = new StompHeaders();
        final String userInfo = stompURI.getUserInfo();
        if (userInfo != null) {
            final String[] parts = userInfo.split(":", 2);
            headers.add(StompHeaders.LOGIN, parts[0]);
            if (parts.length > 1) {
                headers.add(StompHeaders.PASSCODE, parts[1]);
            }
        } else {
            String[] loginAndPasscode = httpRequest.getBasicAuthorization();
            if (loginAndPasscode != null) {
                headers.add(StompHeaders.LOGIN, loginAndPasscode[0]);
                headers.add(StompHeaders.PASSCODE, loginAndPasscode[1]);
            }
        }
        return headers;
    }

    default void latch() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(200);
                latch.countDown();
                System.out.println("Shutting down ...");
            } catch (Exception ignore) {
            }
        }));
        latch.await();
    }

}
