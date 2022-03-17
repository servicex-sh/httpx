package org.mvnsearch.http.protocol.mqtt3;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.protocol.BasePubSubExecutor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Mqtt3SubscriberExecutor implements BasePubSubExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(Mqtt3SubscriberExecutor.class);

    public void subscribeMqtt3(URI mqttURI, HttpRequest httpRequest) {
        MqttClient mqttClient = null;
        try {
            BasePubSubExecutor.UriAndSubject uriAndTopic = getMqttUriAndTopic(mqttURI, httpRequest);
            final String clientId = "httpx-" + UUID.randomUUID();
            mqttClient = new MqttClient(uriAndTopic.uri(), clientId, new MemoryPersistence());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            String[] usernameAndPassword = httpRequest.getBasicAuthorization();
            if (usernameAndPassword != null) {
                connOpts.setUserName(usernameAndPassword[0]);
                connOpts.setPassword(usernameAndPassword[1].toCharArray());
            }
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {

                }

                @Override
                public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) throws Exception {
                    System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received: "));
                    final String content = new String(message.getPayload(), StandardCharsets.UTF_8);
                    if (content.startsWith("{")) { //pretty json output
                        System.out.println(prettyJsonFormat(content));
                    } else {
                        System.out.println(content);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }

                private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

            });
            mqttClient.connect(connOpts);
            mqttClient.subscribe(uriAndTopic.subject(), 1);
            System.out.println("Succeeded to subscribe: " + uriAndTopic.subject() + "!");
            latch();
        } catch (Exception e) {
            log.error("HTX-105-500", mqttURI, e);
        } finally {
            if (mqttClient != null) {
                try {
                    mqttClient.disconnect();
                } catch (MqttException ignore) {
                }
            }
        }
    }

    @Override
    public List<byte[]> execute(HttpRequest httpRequest) {
        return Collections.emptyList();
    }
}
