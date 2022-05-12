package org.mvnsearch.http.protocol;

import com.aliyun.eventbridge.EventBridge;
import com.aliyun.eventbridge.EventBridgeClient;
import com.aliyun.eventbridge.models.CloudEvent;
import com.aliyun.eventbridge.models.Config;
import com.aliyun.eventbridge.models.PutEventsResponse;
import com.aliyun.eventbridge.util.EventBuilder;
import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import io.nats.client.Nats;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.mvnsearch.http.logging.HttpxErrorCodeLogger;
import org.mvnsearch.http.logging.HttpxErrorCodeLoggerFactory;
import org.mvnsearch.http.model.HttpRequest;
import org.mvnsearch.http.protocol.mqtt3.Mqtt3PublisherExecutor;
import org.mvnsearch.http.utils.JsonUtils;
import org.mvnsearch.http.vendor.AWS;
import org.springframework.messaging.simp.stomp.ReactorNettyTcpStompClient;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Sender;
import redis.clients.jedis.Jedis;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.mvnsearch.http.vendor.Aliyun.readAliyunAccessToken;
import static reactor.core.publisher.SignalType.ON_COMPLETE;


public class MessagePublishExecutor implements BasePubSubExecutor {
    private static final HttpxErrorCodeLogger log = HttpxErrorCodeLoggerFactory.getLogger(MessagePublishExecutor.class);

    @Override
    public List<byte[]> execute(HttpRequest httpRequest) {
        final URI realURI = httpRequest.getRequestTarget().getUri();
        String schema = realURI.getScheme();
        String host = realURI.getHost();
        System.out.println("PUB " + realURI);
        System.out.println();
        if (Objects.equals(schema, "kafka")) {
            sendKafka(realURI, httpRequest);
        } else if (Objects.equals(schema, "amqp") || Objects.equals(schema, "amqps")) {
            sendRabbitMQ(realURI, httpRequest);
        } else if (Objects.equals(schema, "nats")) {
            sendNatsMessage(realURI, httpRequest);
        } else if (Objects.equals(schema, "rocketmq")) {
            sendRocketMessage(realURI, httpRequest);
        } else if (Objects.equals(schema, "redis")) {
            sendRedisMessage(realURI, httpRequest);
        } else if (Objects.equals(schema, "pulsar")) {
            sendPulsarMessage(realURI, httpRequest);
        } else if (schema != null && schema.startsWith("mqtt5")) {
            sendMqtt5Message(realURI, httpRequest);
        } else if (schema != null && schema.startsWith("mqtt")) {
            new Mqtt3PublisherExecutor().sendMqtt3Message(realURI, httpRequest);
        } else if (schema != null && schema.startsWith("stomp")) {
            sendStompMessage(realURI, httpRequest);
        } else if (Objects.equals(schema, "eventbridge")) {
            if (host.contains(".eventbridge.")) { //aliyun
                publishAliyunEventBridge(realURI, httpRequest);
            } else if (httpRequest.getHeader("URI", "").contains(":aws:")) {
                sendAwsEventBridgeMessage(realURI, httpRequest);
            }
        } else if (Objects.equals(schema, "mns") || (host.contains(".mns.") && host.endsWith(".aliyuncs.com"))) {
            sendMnsMessage(realURI, httpRequest);
        } else if (Objects.equals(schema, "sns")) {
            sendAwsSnsMessage(realURI, httpRequest);
        } else if (Objects.equals(schema, "sqs")) {
            sendAwsSqsMessage(realURI, httpRequest);
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
                .doOnError(e -> log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e))
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
            final UriAndSubject rabbitUriAndQueue = getRabbitUriAndQueue(rabbitURI, httpRequest);
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.useNio();
            connectionFactory.setUri(rabbitUriAndQueue.uri());
            reactor.rabbitmq.SenderOptions senderOptions = new reactor.rabbitmq.SenderOptions()
                    .connectionFactory(connectionFactory)
                    .resourceManagementScheduler(Schedulers.immediate());
            String contentType = httpRequest.getHeader("Content-Type", "text/plain");
            final AMQP.BasicProperties headers = new AMQP.BasicProperties.Builder().contentType(contentType).build();
            try (Sender rabbitSender = RabbitFlux.createSender(senderOptions)) {
                rabbitSender
                        .send(Mono.just(new OutboundMessage("", rabbitUriAndQueue.subject(), headers, httpRequest.getBodyBytes())))
                        .doOnError(e -> log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e))
                        .doFinally(signalType -> {
                            if (signalType == ON_COMPLETE) {
                                System.out.print("Succeeded to send message to " + rabbitUriAndQueue.subject() + "!");
                            }
                        }).block();
            } catch (Exception ignore) {
                log.error("HTX-105-401", httpRequest.getRequestTarget().getUri());
            }
        } catch (Exception ignore) {
            log.error("HTX-105-401", httpRequest.getRequestTarget().getUri());
        }
    }

    public void sendStompMessage(URI stompURI, HttpRequest httpRequest) {
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
            stompSession.send(topic, httpRequest.getBodyBytes());
            System.out.print("Succeeded to send message to " + topic + "!");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("HTX-105-401", httpRequest.getRequestTarget().getUri());
        } finally {
            if (stompSession != null) {
                stompSession.disconnect();
            }
            if (stompClient != null) {
                stompClient.shutdown();
            }
        }
    }

    public void sendNatsMessage(URI natsURI, HttpRequest httpRequest) {
        String topic = natsURI.getPath().substring(1);
        byte[] body = httpRequest.getBodyBytes();
        try (io.nats.client.Connection nc = Nats.connect(natsURI.toString())) {
            for (String part : topic.split("[,;]")) {
                if (!part.isEmpty()) {
                    nc.publish(part, body);
                }
            }
            System.out.print("Succeeded to send message to " + topic + "!");
        } catch (Exception e) {
            log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void sendPulsarMessage(URI pulsarURI, HttpRequest httpRequest) {
        String topic = pulsarURI.getPath().substring(1);
        try (PulsarClient client = PulsarClient.builder().serviceUrl(pulsarURI.toString()).build();
             Producer<byte[]> producer = client.newProducer().topic(topic).create()) {
            final MessageId msgId = producer.send(httpRequest.getBodyBytes());
            System.out.print("Succeeded to send message to " + topic + " with id " + msgId);
        } catch (Exception e) {
            log.error("HTX-105-500", pulsarURI.toString(), e);
        }
    }

    public void sendRocketMessage(URI rocketURI, HttpRequest httpRequest) {
        DefaultMQProducer producer = new DefaultMQProducer("httpx-cli");
        try {
            // Specify name server addresses.
            String nameServerAddress = rocketURI.getHost() + ":" + rocketURI.getPort();
            String topic = rocketURI.getPath().substring(1);
            producer.setNamesrvAddr(nameServerAddress);
            //Launch the instance.
            producer.start();
            Message msg = new Message(topic, httpRequest.getBodyBytes());
            msg.putUserProperty("Content-Type", httpRequest.getHeader("Content-Type", "text/plain"));
            //Call send message to deliver message to one of brokers.
            SendResult sendResult = producer.send(msg);
            System.out.println("Succeeded to send message to " + topic + "!");
            System.out.print(sendResult.toString());
        } catch (Exception e) {
            log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
        } finally {
            producer.shutdown();
        }
    }


    @SuppressWarnings("unchecked")
    public void publishAliyunEventBridge(URI eventBridgeURI, HttpRequest httpRequest) {
        org.mvnsearch.http.vendor.CloudAccount cloudAccount = readAliyunAccessToken(httpRequest);
        if (cloudAccount == null) {
            System.err.println("Please supply access key Id/Secret in Authorization header as : `Authorization: Basic keyId:secret`");
            return;
        }
        try {
            String eventBus = eventBridgeURI.getPath().substring(1);
            final Map<String, Object> cloudEvent = JsonUtils.readValue(httpRequest.getBodyBytes(), Map.class);
            //validate cloudEvent
            String source = (String) cloudEvent.get("source");
            if (source == null) {
                System.err.println("Please supply source field in json body!");
                return;
            }
            String datacontenttype = (String) cloudEvent.get("datacontenttype");
            if (datacontenttype != null && !datacontenttype.startsWith("application/json")) {
                System.err.println("datacontenttype value should be 'application/json'!");
                return;
            }
            final Object data = cloudEvent.get("data");
            if (data == null) {
                System.err.println("data field should be supplied in json body!");
                return;
            }
            String jsonData;
            if (data instanceof Map<?, ?> || data instanceof List<?>) {
                jsonData = JsonUtils.writeValueAsString(data);
            } else {
                jsonData = data.toString();
            }
            String eventId = (String) cloudEvent.get("id");
            if (eventId == null) {
                eventId = UUID.randomUUID().toString();
            }
            Config authConfig = new Config();
            authConfig.accessKeyId = cloudAccount.getAccessKeyId();
            authConfig.accessKeySecret = cloudAccount.getAccessKeySecret();
            authConfig.endpoint = eventBridgeURI.getHost();
            EventBridge eventBridgeClient = new EventBridgeClient(authConfig);
            final CloudEvent event = EventBuilder.builder()
                    .withId(eventId)
                    .withSource(URI.create(source))
                    .withType((String) cloudEvent.get("type"))
                    .withSubject((String) cloudEvent.get("subject"))
                    .withTime(new Date())
                    .withJsonStringData(jsonData)
                    .withAliyunEventBus(eventBus)
                    .build();
            System.out.println("Begin to send message to " + eventBus + " with '" + eventId + "' ID");
            PutEventsResponse putEventsResponse = eventBridgeClient.putEvents(List.of(event));
            System.out.println("Succeeded with Aliyun EventBridge Response:");
            System.out.println(JsonUtils.writeValueAsPrettyString(putEventsResponse));
        } catch (Exception e) {
            log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void sendMnsMessage(URI mnsURI, HttpRequest httpRequest) {
        org.mvnsearch.http.vendor.CloudAccount cloudAccount = readAliyunAccessToken(httpRequest);
        if (cloudAccount == null) {
            System.err.println("Please supply access key Id/Secret in Authorization header as : `Authorization: Basic keyId:secret`");
            return;
        }
        try {
            String topic = mnsURI.getPath().substring(1);
            final MNSClient mnsClient = new CloudAccount(cloudAccount.getAccessKeyId(), cloudAccount.getAccessKeySecret(), "https://" + mnsURI.getHost()).getMNSClient();
            final CloudQueue queueRef = mnsClient.getQueueRef(topic);
            final com.aliyun.mns.model.Message message = queueRef.putMessage(new com.aliyun.mns.model.Message(httpRequest.getBodyBytes()));
            System.out.println("Succeeded to send message to " + topic + " with ID: " + message.getMessageId());
        } catch (Exception e) {
            log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void sendRedisMessage(URI redisURI, HttpRequest httpRequest) {
        final UriAndSubject redisUriAndChannel = getRedisUriAndChannel(redisURI, httpRequest);
        try (Jedis jedis = new Jedis(redisUriAndChannel.uri())) {
            jedis.publish(redisUriAndChannel.subject().getBytes(), httpRequest.getBodyBytes());
            System.out.print("Succeeded to send message to " + redisUriAndChannel.subject() + "!");
        } catch (Exception e) {
            log.error("HTX-105-500", redisUriAndChannel.uri(), e);
        }
    }

    public void sendMqtt5Message(URI mqttURI, HttpRequest httpRequest) {
        MqttClient mqttClient = null;
        try {
            UriAndSubject uriAndTopic = getMqttUriAndTopic(mqttURI, httpRequest);
            mqttClient = new MqttClient(uriAndTopic.uri(), "httpx-cli", new MemoryPersistence());
            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            connOpts.setCleanStart(true);
            String[] usernameAndPassword = httpRequest.getBasicAuthorization();
            if (usernameAndPassword != null) {
                connOpts.setUserName(usernameAndPassword[0]);
                connOpts.setPassword(usernameAndPassword[1].getBytes(StandardCharsets.UTF_8));
            }
            mqttClient.connect(connOpts);
            final MqttMessage message = new MqttMessage(httpRequest.getBodyBytes());
            final String contentType = httpRequest.getHeader("Content-Type");
            if (contentType != null) {
                message.setProperties(new MqttProperties());
                message.getProperties().setContentType(contentType);
            }
            mqttClient.publish(uriAndTopic.subject(), message);
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

    public void sendAwsSnsMessage(URI snsUri, HttpRequest httpRequest) {
        String topic = httpRequest.getRequestLine();
        final AwsBasicCredentials awsBasicCredentials = AWS.awsBasicCredentials(httpRequest);
        if (awsBasicCredentials == null) {
            System.out.println("Cannot find AWS AK info, please check");
            return;
        }
        String topicArn = httpRequest.getHeader("URI");
        String regionId = getAwsRegionId(httpRequest, topicArn);
        try (SnsClient snsClient = SnsClient.builder()
                .region(Region.of(regionId))
                .credentialsProvider(() -> awsBasicCredentials)
                .build()) {
            PublishRequest request = PublishRequest.builder()
                    .message(new String(httpRequest.getBodyBytes(), StandardCharsets.UTF_8))
                    .topicArn(topicArn)
                    .build();
            PublishResponse result = snsClient.publish(request);
            final SdkHttpResponse response = result.sdkHttpResponse();
            if (response.isSuccessful()) {
                System.out.print("Succeeded to send message to " + topic + "");
            } else {
                System.out.print("Failed to send message to " + topic + ":");
                System.out.println(response.statusText().get());
            }
        }
    }

    public void sendAwsEventBridgeMessage(URI snsUri, HttpRequest httpRequest) {
        String topic = httpRequest.getRequestLine();
        final AwsBasicCredentials awsBasicCredentials = AWS.awsBasicCredentials(httpRequest);
        if (awsBasicCredentials == null) {
            System.out.println("Cannot find AWS AK info, please check");
            return;
        }
        String eventBusArn = httpRequest.getHeader("URI");
        String regionId = getAwsRegionId(httpRequest, eventBusArn);
        try (software.amazon.awssdk.services.eventbridge.EventBridgeClient eventBrClient = software.amazon.awssdk.services.eventbridge.EventBridgeClient.builder()
                .region(Region.of(regionId))
                .credentialsProvider(() -> awsBasicCredentials)
                .build()) {
            final Map<String, Object> cloudEvent = JsonUtils.readValue(httpRequest.getBodyBytes(), Map.class);
            //validate cloudEvent
            String source = (String) cloudEvent.get("source");
            if (source == null) {
                System.err.println("Please supply source field in json body!");
                return;
            }
            String datacontenttype = (String) cloudEvent.get("datacontenttype");
            if (datacontenttype != null && !datacontenttype.startsWith("application/json")) {
                System.err.println("datacontenttype value should be 'application/json'!");
                return;
            }
            final Object data = cloudEvent.get("data");
            if (data == null) {
                System.err.println("data field should be supplied in json body!");
                return;
            }
            String jsonData;
            if (data instanceof Map<?, ?> || data instanceof List<?>) {
                jsonData = JsonUtils.writeValueAsString(data);
            } else {
                jsonData = data.toString();
            }
            PutEventsRequestEntry reqEntry = PutEventsRequestEntry.builder()
                    .resources(eventBusArn)
                    .source(source)
                    .detailType(datacontenttype)
                    .detail(jsonData)
                    .build();
            PutEventsRequest eventsRequest = PutEventsRequest.builder()
                    .entries(reqEntry)
                    .build();
            software.amazon.awssdk.services.eventbridge.model.PutEventsResponse result = eventBrClient.putEvents(eventsRequest);
            for (PutEventsResultEntry resultEntry : result.entries()) {
                if (resultEntry.eventId() != null) {
                    System.out.print("Succeeded to send message to " + topic + " with ID " + resultEntry.eventId());
                } else {
                    System.out.println("Failed to send message to " + topic + ":" + resultEntry.errorCode());
                }
            }
        } catch (Exception e) {
            log.error("HTX-105-500", httpRequest.getRequestTarget().getUri(), e);
        }
    }

    public void sendAwsSqsMessage(URI snsUri, HttpRequest httpRequest) {
        String queue = httpRequest.getRequestLine();
        final AwsBasicCredentials awsBasicCredentials = AWS.awsBasicCredentials(httpRequest);
        if (awsBasicCredentials == null) {
            System.out.println("Cannot find AWS AK info, please check");
            return;
        }
        String queueArn = httpRequest.getHeader("URI");
        String regionId = getAwsRegionId(httpRequest, queueArn);
        String queueUrl;
        if (queueArn != null) {
            final String[] parts = queueArn.split(":");
            String sqsRegionId = parts[3];
            String sqsQueueId = parts[4];
            String sqsName = parts[5];
            queueUrl = "https://sqs." + sqsRegionId + ".amazonaws.com/" + sqsQueueId + "/" + sqsName;
        } else {
            System.out.println("SQS URI is not correct: " + queueArn);
            return;
        }
        try (SqsClient sqsClient = SqsClient.builder()
                .region(Region.of(regionId))
                .credentialsProvider(() -> awsBasicCredentials)
                .build()) {
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(httpRequest.bodyText())
                    .build();
            final SendMessageResponse response = sqsClient.sendMessage(sendMsgRequest);
            System.out.print("Succeeded to send message to " + queue + " with ID " + response.messageId());
        }
    }

    private String getAwsRegionId(HttpRequest httpRequest, String resourceArn) {
        String regionId = httpRequest.getHeader("X-Region-Id");
        if (regionId == null && resourceArn != null) {
            final String[] parts = resourceArn.split(":");
            if (parts.length > 3) {
                regionId = parts[3];
            }
        }
        if (regionId == null) {
            regionId = Region.US_EAST_1.id();
        }
        return regionId;
    }

}
