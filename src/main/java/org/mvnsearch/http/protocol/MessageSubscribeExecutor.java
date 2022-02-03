package org.mvnsearch.http.protocol;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;


public class MessageSubscribeExecutor implements BaseExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(MessageSubscribeExecutor.class);

    @Override
    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI realURI = httpRequest.getRequestTarget().getUri();
        String schema = realURI.getScheme();
        if (Objects.equals(realURI.getScheme(), "kafka")) {
            subscribeKafka(realURI, httpRequest);
        } else if (Objects.equals(schema, "amqp") || Objects.equals(schema, "amqps")) {
            subscribeRabbit(realURI, httpRequest);
        } else if (Objects.equals(schema, "nats")) {
            subscribeNats(realURI, httpRequest);
        } else {
            System.err.println("Not support: " + realURI);
        }
        return Collections.emptyList();
    }

    public void subscribeKafka(URI kafkaURI, HttpRequest httpRequest) {
        Properties props = new Properties();
        int port = kafkaURI.getPort();
        if (port <= 0) {
            port = 9092;
        }
        String topic = kafkaURI.getPath().substring(1);
        final Map<String, String> params = queryToMap(kafkaURI);
        String groupId = "httpx-consumer";
        if (params.containsKey("group")) {
            groupId = params.get("group");
        }
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaURI.getHost() + ":" + port);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            // shutdown hook to properly close the consumer
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    consumer.close();
                } catch (Exception ignore) {

                }
                System.out.println("Shutting down ...");
            }));
            System.out.println("Succeeded to subscribe(1000 max): " + topic + "!");
            // max message count to process
            int counter = 0;
            do {
                ConsumerRecords<String, String> records = consumer.poll(10000);
                for (ConsumerRecord<String, String> record : records) {
                    if (counter > 0) {
                        System.out.println("==================================");
                    }
                    String key = record.key();
                    System.out.println("Received message: " + (key == null ? "" : key));
                    System.out.println(record.value());
                    counter++;
                }
            } while (counter <= 1000);
        } catch (Exception e) {
            log.error("HTX-106-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void subscribeRabbit(URI rabbitURI, HttpRequest httpRequest) {
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
                //channel.queueDeclare(queue, false, false, false, null);
                System.out.println("SUB " + connectionUri);
                System.out.println();
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] Received '" + message + "'");
                };
                channel.basicConsume(queue, true, deliverCallback, consumerTag -> {
                });
            } catch (Exception e) {
                log.error("HTX-105-500", connectionUri, e);
            }
        } catch (Exception e) {
            log.error("HTX-106-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void subscribeNats(URI natsURI, HttpRequest httpRequest) {
        String topic = natsURI.getPath().substring(1);
        try (io.nats.client.Connection nc = Nats.connect(natsURI.toString())) {
            Subscription sub = nc.subscribe(topic);
            nc.flush(Duration.ofSeconds(5));
            System.out.println("Succeeded to subscribe(1000 max): " + topic + "!");
            for (int i = 0; i < 1000; i++) {
                Message msg = sub.nextMessage(Duration.ofHours(1));
                if (i > 0) {
                    System.out.println("======================================");
                }
                System.out.printf("Message Received [%d]\n", (i + 1));
                if (msg.hasHeaders()) {
                    System.out.println("  Headers:");
                    for (String key : msg.getHeaders().keySet()) {
                        for (String value : msg.getHeaders().get(key)) {
                            System.out.printf("    %s: %s\n", key, value);
                        }
                    }
                }
                System.out.printf("  Subject: %s\n  Data: %s\n",
                        msg.getSubject(),
                        new String(msg.getData(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("HTX-106-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }
}
