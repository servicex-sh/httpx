package org.mvnsearch.http.protocol.mqtt3;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.protocol.BasePubSubExecutor;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public class Mqtt3PublisherExecutor implements BasePubSubExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(Mqtt3PublisherExecutor.class);

    @Override
    public List<byte[]> execute(HttpRequest httpRequest) {
        return Collections.emptyList();
    }

    public void sendMqtt3Message(URI mqttURI, HttpRequest httpRequest) {
        MqttClient mqttClient = null;
        try {
            UriAndSubject uriAndTopic = getMqttUriAndTopic(mqttURI, httpRequest);
            mqttClient = new MqttClient(uriAndTopic.uri(), "httpx-cli", new MemoryPersistence());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            String[] usernameAndPassword = httpRequest.getBasicAuthorization();
            if (usernameAndPassword != null) {
                connOpts.setUserName(usernameAndPassword[0]);
                connOpts.setPassword(usernameAndPassword[1].toCharArray());
            }
            mqttClient.connect(connOpts);
            mqttClient.publish(uriAndTopic.subject(), new MqttMessage(httpRequest.getBodyBytes()));
            System.out.print("Succeeded to send message to " + uriAndTopic.subject() + "!");
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
}
