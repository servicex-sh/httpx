package org.mvnsearch.http.protocol;

import com.rabbitmq.client.ConnectionFactory;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.jetbrains.annotations.NotNull;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.protocol.mqtt3.Mqtt3SubscriberExecutor;
import org.springframework.messaging.simp.stomp.*;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;


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
        } else if (Objects.equals(schema, "zeromq")) {
            subscribeZeroMQ(realURI, httpRequest);
        } else if (Objects.equals(schema, "redis")) {
            subscribeRedis(realURI, httpRequest);
        } else if (Objects.equals(schema, "pulsar")) {
            subscribePulsar(realURI, httpRequest);
        } else if (Objects.equals(schema, "rocketmq")) {
            subscribeRocketmq(realURI, httpRequest);
        } else if (schema != null && schema.startsWith("mqtt5")) {
            subscribeMqtt5(realURI, httpRequest);
        } else if (schema != null && schema.startsWith("mqtt")) {
            new Mqtt3SubscriberExecutor().subscribeMqtt3(realURI, httpRequest);
        } else if (Objects.equals(schema, "stomp")) {
            subscribeStomp(realURI, httpRequest);
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
                        System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received: " + (key == null ? "" : key)));
                        System.out.println(prettyJsonFormat(record.value()));
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
                    .connectionSubscriptionScheduler(Schedulers.immediate());
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
                        System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received: "));
                        System.out.println(prettyJsonFormat(new String(delivery.getBody(), StandardCharsets.UTF_8)));
                    })
                    .blockLast();
        } catch (Exception e) {
            log.error("HTX-105-401", httpRequest.getRequestTarget().getUri());
        }
    }

    public void subscribeNats(URI natsURI, HttpRequest httpRequest) {
        String topic = natsURI.getPath().substring(1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        try (io.nats.client.Connection nc = Nats.connect(natsURI.toString())) {
            Dispatcher dispatcher = nc.createDispatcher((msg) -> {
                System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received from " + msg.getSubject()));
                System.out.println(prettyJsonFormat(new String(msg.getData(), StandardCharsets.UTF_8)));
            });
            for (String part : topic.split("[,;]")) {
                if (!part.isEmpty()) {
                    dispatcher.subscribe(part);
                }
            }
            System.out.println("Succeeded to subscribe: " + topic + "!");
            latch();
        } catch (Exception e) {
            log.error("HTX-106-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void subscribeZeroMQ(URI zeromqURI, HttpRequest httpRequest) {
        String topic = zeromqURI.getPath().substring(1);
        String zeromqTopic = topic.equals("*") ? "" : topic;
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        try (ZContext context = new ZContext()) {
            ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
            String connectUri = "tcp://" + zeromqURI.getHost() + ":" + zeromqURI.getPort();
            subscriber.connect(connectUri);
            subscriber.subscribe(topic.equals("*") ? "" : topic);
            System.out.println("Succeeded to subscribe: " + topic + "!");
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received: "));
                String content = subscriber.recvStr();
                if (!zeromqTopic.isEmpty() && content.length() > zeromqTopic.length() + 1) {
                    content = content.substring(zeromqTopic.length()).trim();
                }
                System.out.println(prettyJsonFormat(content));
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
                    System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received: "));
                    System.out.println(prettyJsonFormat(message));
                }
            }, redisUriAndChannel.subject());
        }
    }

    public void subscribePulsar(URI pulsarURI, HttpRequest httpRequest) {
        String topic = pulsarURI.getPath().substring(1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        try (PulsarClient client = PulsarClient.builder().serviceUrl(pulsarURI.toString()).build();
             Consumer<byte[]> ignore = client.newConsumer()
                     .topic(topic)
                     .subscriptionName("httpx-cli-" + UUID.randomUUID())
                     .messageListener((consumer, msg) -> {
                         try {
                             System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received: "));
                             System.out.println(prettyJsonFormat(new String(msg.getData(), StandardCharsets.UTF_8)));
                             consumer.acknowledge(msg);
                         } catch (Exception e) {
                             consumer.negativeAcknowledge(msg);
                         }
                     })
                     .subscribe()
        ) {
            System.out.println("Succeeded to subscribe: " + topic + "!");
            latch();
        } catch (Exception e) {
            log.error("HTX-105-500", pulsarURI.toString(), e);
        }
    }

    public void subscribeMqtt5(URI mqttURI, HttpRequest httpRequest) {
        MqttClient mqttClient = null;
        try {
            UriAndSubject uriAndTopic = getMqttUriAndTopic(mqttURI, httpRequest);
            final String clientId = "httpx-" + UUID.randomUUID();
            mqttClient = new MqttClient(uriAndTopic.uri(), clientId, new MemoryPersistence());
            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            connOpts.setCleanStart(true);
            String[] usernameAndPassword = httpRequest.getBasicAuthorization();
            if (usernameAndPassword != null) {
                connOpts.setUserName(usernameAndPassword[0]);
                connOpts.setPassword(usernameAndPassword[1].getBytes(StandardCharsets.UTF_8));
            }
            mqttClient.setCallback(new AbstractMqttCallback() {
                private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received: "));
                    final String content = new String(message.getPayload(), StandardCharsets.UTF_8);
                    final MqttProperties messageProperties = message.getProperties();
                    String contentType = "text/plain";
                    if (messageProperties != null && messageProperties.getContentType() != null) {
                        contentType = messageProperties.getContentType();
                    }
                    if (content.startsWith("{") || contentType.contains("json")) { //pretty json output
                        System.out.println(prettyJsonFormat(content));
                    } else {
                        System.out.println(content);
                    }
                }
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

    public void subscribeStomp(URI stompURI, HttpRequest httpRequest) {
        ReactorNettyTcpStompClient stompClient = null;
        StompSession stompSession = null;
        try {
            String topic = stompURI.getPath().substring(1);
            int port = stompURI.getPort();
            if (port <= 0) {
                port = 61613;
            }
            stompClient = new ReactorNettyTcpStompClient(stompURI.getHost(), port);
            stompSession = stompClient.connect(constructStompHeaders(stompURI, httpRequest), new StompSessionHandlerAdapter() {
            }).get();
            stompSession.subscribe(topic, new StompFrameHandler() {
                private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

                @Override
                public @NotNull Type getPayloadType(@NotNull StompHeaders headers) {
                    return Object.class;
                }

                @Override
                public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                    System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received: "));
                    String content;
                    if (payload instanceof byte[]) {
                        content = new String((byte[]) payload, StandardCharsets.UTF_8);
                    } else {
                        content = payload.toString();
                    }
                    if (content.startsWith("{")) { //pretty json output
                        System.out.println(prettyJsonFormat(content));
                    } else {
                        System.out.println(content);
                    }
                }
            });
            System.out.println("Succeeded to subscribe " + topic + "!");
            latch();
        } catch (Exception e) {
            log.error("HTX-105-401", httpRequest.getRequestTarget().getUri(), e);
        } finally {
            if (stompSession != null) {
                stompSession.disconnect();
            }
            if (stompClient != null) {
                stompClient.shutdown();
            }
        }
    }

    public void subscribeRocketmq(URI rocketURI, HttpRequest httpRequest) {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("httpx-" + UUID.randomUUID());
        try {
            String nameServerAddress = rocketURI.getHost() + ":" + rocketURI.getPort();
            String topic = rocketURI.getPath().substring(1);
            consumer.setNamesrvAddr(nameServerAddress);
            consumer.subscribe(topic, "*");
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            // Register callback to execute on arrival of messages fetched from brokers.
            consumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
                for (MessageExt messageExt : msgList) {
                    System.out.println(colorOutput("bold,green", dateFormat.format(new Date()) + " message received: " + messageExt.getMsgId())
                            + "\n"
                            + prettyJsonFormat(new String(messageExt.getBody(), StandardCharsets.UTF_8))
                    );
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();
            System.out.println("Succeeded to subscribe " + topic + "!");
            latch();
        } catch (Exception e) {
            log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
        } finally {
            consumer.shutdown();
        }
    }

}
