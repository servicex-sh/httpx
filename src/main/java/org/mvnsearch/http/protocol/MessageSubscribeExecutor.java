package org.mvnsearch.http.protocol;

import com.rabbitmq.client.ConnectionFactory;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;


public class MessageSubscribeExecutor implements BasePubSubExecutor {
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
        } else if (Objects.equals(schema, "redis")) {
            subscribeRedis(realURI, httpRequest);
        } else if (Objects.equals(schema, "rocketmq")) {
            subscribeRocketmq(realURI, httpRequest);
        } else if (schema != null && schema.startsWith("mqtt")) {
            subscribeMqtt(realURI, httpRequest);
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
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            receiver.receive()
                    .doOnSubscribe(subscription -> {
                        System.out.println("Succeeded to subscribe: " + topic + "!");
                    })
                    .doOnNext(record -> {
                        String key = record.key();
                        System.out.println(dateFormat.format(new Date()) + " message received: " + (key == null ? "" : key));
                        System.out.println(record.value());
                    })
                    .blockLast();
        } catch (Exception e) {
            log.error("HTX-106-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void subscribeRabbit(URI rabbitURI, HttpRequest httpRequest) {
        try {
            final UriAndSubject rabbitUriAndQueue = getRabbitUriAndQueue(rabbitURI, httpRequest);
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setUri(rabbitUriAndQueue.uri());
            ReceiverOptions receiverOptions = new ReceiverOptions()
                    .connectionFactory(connectionFactory)
                    .connectionSubscriptionScheduler(Schedulers.boundedElastic());
            final Receiver receiver = RabbitFlux.createReceiver(receiverOptions);
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            receiver.consumeAutoAck(rabbitUriAndQueue.subject())
                    .doOnSubscribe(subscription -> {
                        System.out.println("SUB " + rabbitUriAndQueue.uri());
                        System.out.println();
                    })
                    .doOnError(e -> {
                        log.error("HTX-106-500", httpRequest.getRequestTarget().getUri(), e);
                    })
                    .doOnNext(delivery -> {
                        System.out.println(dateFormat.format(new Date()) + " message received: ");
                        System.out.println(new String(delivery.getBody(), StandardCharsets.UTF_8));
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
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            System.out.println("Succeeded to subscribe(1000 max): " + topic + "!");
            for (int i = 0; i < 1000; i++) {
                Message msg = sub.nextMessage(Duration.ofHours(1));
                if (i > 0) {
                    System.out.println("======================================");
                }
                System.out.printf(dateFormat.format(new Date()) + " message received: [%d]\n", (i + 1));
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

    public void subscribeRedis(URI redisURI, HttpRequest httpRequest) {
        final UriAndSubject redisUriAndChannel = getRedisUriAndChannel(redisURI, httpRequest);
        try (Jedis jedis = new Jedis(redisUriAndChannel.uri())) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            System.out.println("Succeeded to subscribe: " + redisUriAndChannel.subject() + "!");
            jedis.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    System.out.println(dateFormat.format(new Date()) + " message received: ");
                    System.out.println(message);
                }
            }, redisUriAndChannel.subject());
        }
    }

    public void subscribeMqtt(URI mqttURI, HttpRequest httpRequest) {
        MqttClient mqttClient = null;
        try {
            UriAndSubject uriAndTopic = getMqttUriAndTopic(mqttURI, httpRequest);
            final String clientId = "httpx-" + UUID.randomUUID();
            mqttClient = new MqttClient(uriAndTopic.uri(), clientId, new MemoryPersistence());
            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            connOpts.setCleanStart(true);
            mqttClient.setCallback(new AbstractMqttCallback() {
                private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received: "));
                    final String content = new String(message.getPayload(), StandardCharsets.UTF_8);
                    if (content.startsWith("{")) { //pretty json output
                        System.out.println(prettyJsonFormat(content));
                    } else {
                        System.out.println(content);
                    }
                }
            });
            mqttClient.connect(connOpts);
            mqttClient.subscribe(uriAndTopic.subject(), 1);
            System.out.println("Succeeded to subscribe: " + uriAndTopic.subject() + "!");
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

    public void subscribeRocketmq(URI rocketURI, HttpRequest httpRequest) {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("httpx-" + UUID.randomUUID());
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Thread.sleep(200);
                    latch.countDown();
                    System.out.println("Shutting down ...");
                } catch (Exception ignore) {
                }
            }));
            String nameServerAddress = rocketURI.getHost() + ":" + rocketURI.getPort();
            String topic = rocketURI.getPath().substring(1);
            consumer.setNamesrvAddr(nameServerAddress);
            consumer.subscribe(topic, "*");
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            // Register callback to execute on arrival of messages fetched from brokers.
            consumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
                for (MessageExt messageExt : msgList) {
                    System.out.println(dateFormat.format(new Date()) + " message received: " + messageExt.getMsgId());
                    System.out.println(new String(messageExt.getBody(), StandardCharsets.UTF_8));
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();
            System.out.println("Succeeded to subscribe(1000 max): " + topic + "!");
            latch.await();
        } catch (Exception e) {
            log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
        } finally {
            consumer.shutdown();
        }
    }
}
