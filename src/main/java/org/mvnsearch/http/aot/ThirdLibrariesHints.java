package org.mvnsearch.http.aot;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.aliyun.credentials.models.Config;
import com.aliyun.eventbridge.models.CloudEvent;
import com.aliyun.eventbridge.models.PutEventsResponse;
import com.aliyun.eventbridge.models.PutEventsResponseEntry;
import com.aliyun.mns.client.DefaultMNSClient;
import com.aliyun.mns.client.MNSClient;
import com.aliyun.tea.TeaModel;
import com.aliyun.tea.TeaPair;
import com.aliyun.tea.TeaRequest;
import com.aliyun.tea.TeaResponse;
import com.aliyuncs.AcsRequest;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.UserAgentConfig;
import com.aliyuncs.http.clients.ApacheHttpClient;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.sun.mail.util.MailSSLSocketFactory;
import io.nats.client.impl.SocketDataPort;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.rocketmq.common.protocol.header.SendMessageRequestHeaderV2;
import org.apache.rocketmq.common.protocol.header.SendMessageResponseHeader;
import org.apache.rocketmq.common.protocol.header.UnregisterClientRequestHeader;
import org.apache.rocketmq.common.protocol.header.namesrv.GetRouteInfoRequestHeader;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.netty.NettyDecoder;
import org.apache.rocketmq.remoting.netty.NettyEncoder;
import org.apache.rocketmq.remoting.protocol.*;
import org.eclipse.paho.mqttv5.client.internal.SSLNetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.internal.TCPNetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.logging.JSR47Logger;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.client.spi.NetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.websocket.WebSocketNetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.websocket.WebSocketSecureNetworkModuleFactory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLSocketFactory;


public class ThirdLibrariesHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints registry, ClassLoader classLoader) {
        // javamail
        registry.reflection().registerType(MailSSLSocketFactory.class);
        registry.reflection().registerType(SSLSocketFactory.class);
        final Class<?>[] kafkaClassArray = {ConsumerConfig.class, KafkaConsumer.class, RangeAssignor.class,
                KafkaProducer.class, ProducerConfig.class, ProducerRecord.class, DefaultPartitioner.class,
                LongDeserializer.class, LongSerializer.class, StringDeserializer.class, StringSerializer.class,
                CooperativeStickyAssignor.class};
        for (Class<?> clazz : kafkaClassArray) {
            registry.reflection().registerType(clazz);
        }
        registry.resources().registerResource(new ClassPathResource("kafka/kafka-version.properties"));
        //rabbitmq
        registry.resources().registerResource(new ClassPathResource("rabbitmq-amqp-client.properties"));
        //nats
        registry.reflection().registerType(SocketDataPort.class);
        //aliyun event bridge
        final Class<?>[] aliyunClassArray = {com.aliyun.credentials.models.Config.class, com.aliyun.eventbridge.models.Config.class,
                CloudEvent.class, PutEventsResponse.class, PutEventsResponseEntry.class,
                TeaModel.class, TeaRequest.class, TeaResponse.class, TeaPair.class,};
        for (Class<?> clazz : aliyunClassArray) {
            registry.reflection().registerType(clazz);
        }
        // aliyun core sdk
        final Class<?>[] aliyunSdkCoreClassArray = {
                Config.class,
                AcsRequest.class,
                DefaultAcsClient.class,
                IAcsClient.class,
                UserAgentConfig.class,
                ApacheHttpClient.class,
        };
        for (Class<?> clazz : aliyunSdkCoreClassArray) {
            registry.reflection().registerType(clazz);
        }
        registry.resources().registerResource(new ClassPathResource("project.properties"));
        registry.resources().registerResource(new ClassPathResource("endpoints.json"));
        registry.resources().registerResource(new ClassPathResource("regions.txt"));
        // aliyun MNS
        final Class<?>[] mnsClassArray = {MNSClient.class, DefaultMNSClient.class};
        for (Class<?> clazz : mnsClassArray) {
            registry.reflection().registerType(clazz);
        }
        registry.resources().registerResource(new ClassPathResource("versioninfo.properties"));
        registry.resources().registerResourceBundle("common");
        //gson
        final Class<?>[] gsonClassArray = {Gson.class, GsonBuilder.class, TypeToken.class, JsonElement.class};
        for (Class<?> clazz : gsonClassArray) {
            registry.reflection().registerType(clazz);
        }
        //fastjson
        final Class<?>[] fastjsonClassArray = {JSONArray.class, JSONObject.class};
        for (Class<?> clazz : fastjsonClassArray) {
            registry.reflection().registerType(clazz);
        }
        //rocketmq
        final Class<?>[] rocketmqClassArray = {
                org.apache.rocketmq.common.consumer.ConsumeFromWhere.class,
                org.apache.rocketmq.common.protocol.header.GetConsumerListByGroupRequestHeader.class,
                org.apache.rocketmq.common.protocol.header.GetConsumerListByGroupResponseBody.class,
                org.apache.rocketmq.common.protocol.header.NotifyConsumerIdsChangedRequestHeader.class,
                org.apache.rocketmq.common.protocol.header.PullMessageRequestHeader.class,
                org.apache.rocketmq.common.protocol.header.PullMessageResponseHeader.class,
                org.apache.rocketmq.common.protocol.header.QueryConsumerOffsetRequestHeader.class,
                org.apache.rocketmq.common.protocol.header.QueryConsumerOffsetResponseHeader.class,
                org.apache.rocketmq.common.protocol.heartbeat.ConsumeType.class,
                org.apache.rocketmq.common.protocol.heartbeat.ConsumerData.class,
                org.apache.rocketmq.common.protocol.heartbeat.HeartbeatData.class,
                org.apache.rocketmq.common.protocol.heartbeat.MessageModel.class,
                org.apache.rocketmq.common.protocol.heartbeat.ProducerData.class,
                org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData.class,
                SendMessageRequestHeaderV2.class,
                SendMessageResponseHeader.class,
                UnregisterClientRequestHeader.class,
                GetRouteInfoRequestHeader.class,
                BrokerData.class,
                QueueData.class,
                TopicRouteData.class,
                NettyDecoder.class,
                NettyEncoder.class,
                LanguageCode.class,
                RemotingCommand.class,
                RemotingCommandType.class,
                RemotingSerializable.class,
                SerializeType.class};
        for (Class<?> clazz : rocketmqClassArray) {
            registry.reflection().registerType(clazz);
        }
        //eclipse paho mqtt5 client
        registry.resources().registerResource(new ClassPathResource("org/eclipse/paho/mqttv5/logging/JSR47Logger.class"));
        registry.resources().registerResource(new ClassPathResource("org/eclipse/paho/mqttv5/client/internal/nls/logcat.properties"));
        registry.resources().registerResource(new ClassPathResource("org/eclipse/paho/mqttv5/common/nls/logcat.properties"));
        registry.resources().registerResourceBundle("org/eclipse/paho/mqttv5/common/nls/messages");
        final Class<?>[] mqtt5ClassArray = {Logger.class, JSR47Logger.class, LoggerFactory.class,
                NetworkModuleFactory.class, WebSocketNetworkModuleFactory.class,
                WebSocketSecureNetworkModuleFactory.class, SSLNetworkModuleFactory.class,
                TCPNetworkModuleFactory.class};
        for (Class<?> clazz : mqtt5ClassArray) {
            registry.reflection().registerType(clazz);
        }
        //eclipse paho mqtt3 client
        registry.resources().registerResource(new ClassPathResource("org/eclipse/paho/client/mqttv3/logging/JSR47Logger.class"));
        registry.resources().registerResource(new ClassPathResource("org/eclipse/paho/client/mqttv3/internal/nls/logcat.properties"));
        registry.resources().registerResourceBundle("org/eclipse/paho/client/mqttv3/internal/nls/messages");
        final Class<?>[] mqtt3ClassArray = {
                org.eclipse.paho.client.mqttv3.logging.Logger.class,
                org.eclipse.paho.client.mqttv3.logging.JSR47Logger.class,
                org.eclipse.paho.client.mqttv3.logging.LoggerFactory.class,
                org.eclipse.paho.client.mqttv3.spi.NetworkModuleFactory.class,
                org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketNetworkModuleFactory.class,
                org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketSecureNetworkModuleFactory.class,
                org.eclipse.paho.client.mqttv3.internal.SSLNetworkModuleFactory.class,
                org.eclipse.paho.client.mqttv3.internal.TCPNetworkModuleFactory.class,
        };
        for (Class<?> clazz : mqtt3ClassArray) {
            registry.reflection().registerType(clazz);
        }
        //apache pulsar
        final Class<?>[] pulsarClassArray = {
                org.apache.pulsar.client.api.ConsumerCryptoFailureAction.class,
                org.apache.pulsar.client.api.CompressionType.class,
                org.apache.pulsar.client.api.HashingScheme.class,
                org.apache.pulsar.client.api.MessageRoutingMode.class,
                org.apache.pulsar.client.api.ProducerAccessMode.class,
                org.apache.pulsar.client.api.ProducerCryptoFailureAction.class,
                org.apache.pulsar.client.api.ProxyProtocol.class,
                org.apache.pulsar.client.api.RegexSubscriptionMode.class,
                org.apache.pulsar.client.api.SubscriptionInitialPosition.class,
                org.apache.pulsar.client.api.SubscriptionMode.class,
                org.apache.pulsar.client.api.SubscriptionType.class,
                org.apache.pulsar.client.impl.ClientCnx.class,
                org.apache.pulsar.client.impl.PulsarChannelInitializer.class,
                org.apache.pulsar.client.impl.PulsarClientImplementationBindingImpl.class,
                org.apache.pulsar.client.impl.conf.ClientConfigurationData.class,
                org.apache.pulsar.client.impl.conf.ConsumerConfigurationData.class,
                org.apache.pulsar.client.impl.conf.ProducerConfigurationData.class,
                org.apache.pulsar.client.util.SecretsSerializer.class,
                org.apache.pulsar.common.protocol.ByteBufPair.Encoder.class,
                org.apache.pulsar.common.protocol.PulsarDecoder.class,
                io.netty.channel.socket.nio.NioDatagramChannel.class,
                io.netty.channel.socket.nio.NioSocketChannel.class
        };
        for (Class<?> clazz : pulsarClassArray) {
            registry.reflection().registerType(clazz);
        }
        //sofa RPC
        final Class<?>[] sofaClassArray = {SofaRequest.class, SofaResponse.class};
        for (Class<?> clazz : sofaClassArray) {
            registry.reflection().registerType(clazz);
        }
        //jsch by https://github.com/mwiede/jsch
        try {
            final Class<?>[] jschClassArray = {
                    Class.forName("com.jcraft.jsch.DH448"),
                    Class.forName("com.jcraft.jsch.DH25519"),
                    Class.forName("com.jcraft.jsch.DHEC256"),
                    Class.forName("com.jcraft.jsch.DHEC384"),
                    Class.forName("com.jcraft.jsch.DHEC521"),
                    Class.forName("com.jcraft.jsch.DHG1"),
                    Class.forName("com.jcraft.jsch.DHG14"),
                    Class.forName("com.jcraft.jsch.DHG15"),
                    Class.forName("com.jcraft.jsch.DHG16"),
                    Class.forName("com.jcraft.jsch.DHG17"),
                    Class.forName("com.jcraft.jsch.DHG18"),
                    Class.forName("com.jcraft.jsch.DHGEX"),
                    Class.forName("com.jcraft.jsch.DHGEX1"),
                    Class.forName("com.jcraft.jsch.DHGEX224"),
                    Class.forName("com.jcraft.jsch.DHGEX256"),
                    Class.forName("com.jcraft.jsch.DHGEX384"),
                    Class.forName("com.jcraft.jsch.DHGEX512"),
                    Class.forName("com.jcraft.jsch.UserAuthNone"),
                    Class.forName("com.jcraft.jsch.UserAuthPassword"),
                    Class.forName("com.jcraft.jsch.UserAuthPublicKey"),
                    Class.forName("com.jcraft.jsch.jce.AES128CBC"),
                    Class.forName("com.jcraft.jsch.jce.AES128CTR"),
                    Class.forName("com.jcraft.jsch.jce.AES192CBC"),
                    Class.forName("com.jcraft.jsch.jce.AES192CTR"),
                    Class.forName("com.jcraft.jsch.jce.AES256CBC"),
                    Class.forName("com.jcraft.jsch.jce.AES256CTR"),
                    Class.forName("com.jcraft.jsch.jce.DH"),
                    Class.forName("com.jcraft.jsch.jce.ECDHN"),
                    Class.forName("com.jcraft.jsch.jce.HMACSHA1"),
                    Class.forName("com.jcraft.jsch.jce.MD5"),
                    Class.forName("com.jcraft.jsch.jce.Random"),
                    Class.forName("com.jcraft.jsch.jce.SHA1"),
                    Class.forName("com.jcraft.jsch.jce.SHA256"),
                    Class.forName("com.jcraft.jsch.jce.SHA384"),
                    Class.forName("com.jcraft.jsch.jce.SHA512"),
                    Class.forName("com.jcraft.jsch.jce.HMACSHA1"),
                    Class.forName("com.jcraft.jsch.jce.HMACSHA256"),
                    Class.forName("com.jcraft.jsch.jce.HMACSHA256ETM"),
                    Class.forName("com.jcraft.jsch.jce.HMACSHA512"),
                    Class.forName("com.jcraft.jsch.jce.HMACSHA512ETM"),
                    Class.forName("com.jcraft.jsch.jce.SignatureDSA"),
                    Class.forName("com.jcraft.jsch.jce.SignatureECDSA256"),
                    Class.forName("com.jcraft.jsch.jce.SignatureECDSA384"),
                    Class.forName("com.jcraft.jsch.jce.SignatureECDSA521"),
                    Class.forName("com.jcraft.jsch.jce.SignatureRSA"),
                    Class.forName("com.jcraft.jsch.jce.SignatureRSASHA512"),
                    Class.forName("com.jcraft.jsch.jce.SignatureRSASHA256"),
                    Class.forName("com.jcraft.jsch.jce.XDH"),
                    Class.forName("com.jcraft.jsch.jce.TripleDESCBC"),
                    Class.forName("com.jcraft.jsch.jce.TripleDESCTR"),
            };
            for (Class<?> clazz : jschClassArray) {
                registry.reflection().registerType(clazz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //ini4j
        final Class<?>[] ini4jArray = {
                org.ini4j.spi.EscapeTool.class,
                org.ini4j.spi.IniBuilder.class,
                org.ini4j.spi.IniParser.class
        };
        for (Class<?> clazz : ini4jArray) {
            registry.reflection().registerType(clazz);
        }
        //json-smart
        final Class<?>[] jsonSmartArray = {
                net.minidev.json.JSONArray.class,
                net.minidev.json.JSONAware.class,
                net.minidev.json.JSONAwareEx.class,
                net.minidev.json.JSONStreamAware.class,
                net.minidev.json.JSONStreamAwareEx.class
        };
        for (Class<?> clazz : jsonSmartArray) {
            registry.reflection().registerType(clazz);
        }
        //msgpack
        final Class<?>[] msgPackArray = {
                org.msgpack.jackson.dataformat.MessagePackExtensionType.class
        };
        for (Class<?> clazz : msgPackArray) {
            registry.reflection().registerType(clazz);
        }
    }
}
