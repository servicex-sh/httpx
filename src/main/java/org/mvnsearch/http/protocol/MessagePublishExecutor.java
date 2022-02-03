package org.mvnsearch.http.protocol;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class MessagePublishExecutor implements BaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(MessagePublishExecutor.class);

    @Override
    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI realURI = httpRequest.getRequestTarget().getUri();
        String schema = realURI.getScheme();
        System.out.println("PUB " + realURI);
        if (Objects.equals(schema, "kafka")) {
            sendKafka(realURI, httpRequest);
        } else if (Objects.equals(schema, "amqp") || Objects.equals(schema, "amqps")) {
            sendRabbitMQ(realURI, httpRequest);
        } else if (Objects.equals(schema, "rocketmq")) {
            sendRocketMessage(realURI, httpRequest);
        } else {
            System.err.println("Not support: " + realURI);
        }
        return Collections.emptyList();
    }

    public void sendKafka(URI kafkaURI, HttpRequest httpRequest) {
        Properties props = new Properties();
        int port = kafkaURI.getPort();
        if (port <= 0) {
            port = 9092;
        }
        String topic = kafkaURI.getPath().substring(1);
        String body = new String(httpRequest.getBodyBytes(), StandardCharsets.UTF_8);
        Integer partition = null;
        String key = null;
        final Map<String, String> params = queryToMap(kafkaURI);
        if (params.containsKey("key")) {
            key = params.get("key");
        }
        if (params.containsKey("partition")) {
            partition = Integer.valueOf(params.get("partition"));
        }
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaURI.getHost() + ":" + port);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, partition, key, body));
            producer.close();
            System.out.print("Succeeded to send message to " + topic + "!");
        } catch (Exception e) {
            log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void sendRabbitMQ(URI rabbitURI, HttpRequest httpRequest) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            URI connectionUri;
            String queue;
            final String hostHeader = httpRequest.getHeader("Host");
            if (hostHeader != null) {
                connectionUri = URI.create(hostHeader);
                queue = httpRequest.getRequestTarget().getRequestLine();
            } else {
                connectionUri = rabbitURI;
                queue = queryToMap(rabbitURI).get("queue");
            }
            factory.setUri(connectionUri);
            Connection connection = factory.newConnection();
            try (Channel channel = connection.createChannel()) {
                // channel.queueDeclare(queue, false, false, false, null);
                String message = new String(httpRequest.getBodyBytes(), StandardCharsets.UTF_8);
                channel.basicPublish("", queue, null, message.getBytes());
                System.out.print("Succeeded to send message to " + queue + "!");
            } catch (Exception e) {
                log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
            }
            connection.close();
        } catch (Exception ignore) {

        }
    }

    public void sendRocketMessage(URI rsocketURI, HttpRequest httpRequest) {
        System.err.println("Not implemented yet");
    }
}
