package org.mvnsearch.http.protocol;

import com.rabbitmq.client.ConnectionFactory;
import io.nats.client.Nats;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Sender;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static reactor.core.publisher.SignalType.ON_COMPLETE;


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
        } else if (Objects.equals(schema, "nats")) {
            sendNatsMessage(realURI, httpRequest);
        } else if (Objects.equals(schema, "rocketmq")) {
            sendRocketMessage(realURI, httpRequest);
        } else if (Objects.equals(schema, "event")) {
            putEventsSample();
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
        KafkaSender<String, String> sender = KafkaSender.create(SenderOptions.create(props));
        sender.send(Mono.just(SenderRecord.create(topic, partition, System.currentTimeMillis(),
                        key, body, null)))
                .doOnError(e -> {
                    log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
                })
                .doFinally(signalType -> {
                    if (signalType == ON_COMPLETE) {
                        System.out.print("Succeeded to send message to " + topic + "!");
                    }
                    sender.close();
                })
                .blockLast();
    }

    public void sendRabbitMQ(URI rabbitURI, HttpRequest httpRequest) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.useNio();
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
            reactor.rabbitmq.SenderOptions senderOptions = new reactor.rabbitmq.SenderOptions()
                    .connectionFactory(connectionFactory)
                    .resourceManagementScheduler(Schedulers.boundedElastic());

            final Sender rabbitSender = RabbitFlux.createSender(senderOptions);
            rabbitSender
                    .send(Mono.just(new OutboundMessage("", queue, httpRequest.getBodyBytes())))
                    .doOnError(e -> {
                        log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
                    })
                    .doFinally(signalType -> {
                        if (signalType == ON_COMPLETE) {
                            System.out.print("Succeeded to send message to " + queue + "!");
                        }
                        rabbitSender.close();
                    }).block();
        } catch (Exception ignore) {
            log.error("HTX-105-401", httpRequest.getRequestTarget().getUri());
        }
    }

    public void sendNatsMessage(URI natsURI, HttpRequest httpRequest) {
        String topic = natsURI.getPath().substring(1);
        byte[] body = httpRequest.getBodyBytes();
        try (io.nats.client.Connection nc = Nats.connect(natsURI.toString())) {
            nc.publish(topic, body);
            System.out.print("Succeeded to send message to " + topic + "!");
        } catch (Exception e) {
            log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void sendRocketMessage(URI rsocketURI, HttpRequest httpRequest) {
        System.err.println("Not implemented yet");
    }


    public void putEventsSample() {

    }

}
