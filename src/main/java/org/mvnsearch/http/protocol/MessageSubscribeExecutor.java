package org.mvnsearch.http.protocol;

import com.rabbitmq.client.ConnectionFactory;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;

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
        String groupId = "httpx-" + UUID.randomUUID();
        if (params.containsKey("group")) {
            groupId = params.get("group");
        }
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaURI.getHost() + ":" + port);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        reactor.kafka.receiver.ReceiverOptions<String, String> receiverOptions =
                reactor.kafka.receiver.ReceiverOptions.<String, String>create(props).subscription(Collections.singleton(topic));

        try {
            final KafkaReceiver<String, String> receiver = KafkaReceiver.create(receiverOptions);
            receiver.receive()
                    .doOnSubscribe(subscription -> {
                        System.out.println("Succeeded to subscribe: " + topic + "!");
                    })
                    .doOnNext(record -> {
                        String key = record.key();
                        System.out.println("Received message: " + (key == null ? "" : key));
                        System.out.println(record.value());
                    })
                    .blockLast();
        } catch (Exception e) {
            log.error("HTX-106-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void subscribeRabbit(URI rabbitURI, HttpRequest httpRequest) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
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
            connectionFactory.setUri(connectionUri);
            ReceiverOptions receiverOptions = new ReceiverOptions()
                    .connectionFactory(connectionFactory)
                    .connectionSubscriptionScheduler(Schedulers.boundedElastic());
            final Receiver receiver = RabbitFlux.createReceiver(receiverOptions);
            receiver.consumeAutoAck(queue)
                    .doOnSubscribe(subscription -> {
                        System.out.println("SUB " + connectionUri);
                        System.out.println();
                    })
                    .doOnError(e -> {
                        log.error("HTX-106-500", httpRequest.getRequestTarget().getUri(), e);
                    })
                    .doOnNext(delivery -> {
                        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                        System.out.println(" [x] Received '" + message + "'");
                    })
                    .blockLast();
        } catch (Exception e) {
            log.error("HTX-105-401", httpRequest.getRequestTarget().getUri());
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
