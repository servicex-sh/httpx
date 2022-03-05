package org.mvnsearch.http.aot;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import org.apache.pulsar.common.protocol.ByteBufPair;
import org.apache.rocketmq.common.protocol.header.SendMessageRequestHeaderV2;
import org.apache.rocketmq.common.protocol.header.SendMessageResponseHeader;
import org.apache.rocketmq.common.protocol.header.UnregisterClientRequestHeader;
import org.apache.rocketmq.common.protocol.header.namesrv.GetRouteInfoRequestHeader;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.netty.NettyDecoder;
import org.apache.rocketmq.remoting.netty.NettyEncoder;
import org.apache.rocketmq.remoting.protocol.LanguageCode;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.SerializeType;
import org.eclipse.paho.mqttv5.client.internal.SSLNetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.internal.TCPNetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.logging.JSR47Logger;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.client.spi.NetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.websocket.WebSocketNetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.websocket.WebSocketSecureNetworkModuleFactory;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.BeanFactoryNativeConfigurationProcessor;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeResourcesEntry;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.nativex.hint.TypeAccess;

import javax.net.ssl.SSLSocketFactory;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;


public class ThirdLibrariesHints implements BeanFactoryNativeConfigurationProcessor {
    @Override
    public void process(ConfigurableListableBeanFactory beanFactory, NativeConfigurationRegistry registry) {
        // hessian
        registry.reflection().forType(Date.class).withAccess(TypeAccess.DECLARED_CONSTRUCTORS).build();
        registry.reflection().forType(Time.class).withAccess(TypeAccess.DECLARED_CONSTRUCTORS).build();
        registry.reflection().forType(Timestamp.class).withAccess(TypeAccess.DECLARED_CONSTRUCTORS).build();
        // javamail
        registry.reflection().forType(MailSSLSocketFactory.class).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        registry.reflection().forType(SSLSocketFactory.class).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        // kafka
        final Class<?>[] kafkaClassArray = {ConsumerConfig.class, KafkaConsumer.class, RangeAssignor.class,
                KafkaProducer.class, ProducerConfig.class, ProducerRecord.class, DefaultPartitioner.class,
                LongDeserializer.class, LongSerializer.class, StringDeserializer.class, StringSerializer.class,
                CooperativeStickyAssignor.class};
        for (Class<?> clazz : kafkaClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        }
        registry.resources().add(NativeResourcesEntry.of("kafka/kafka-version.properties"));
        //rabbitmq
        registry.resources().add(NativeResourcesEntry.of("rabbitmq-amqp-client.properties"));
        //nats
        registry.reflection().forType(SocketDataPort.class).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        //aliyun event bridge
        final Class<?>[] aliyunClassArray = {com.aliyun.credentials.models.Config.class, com.aliyun.eventbridge.models.Config.class,
                CloudEvent.class, PutEventsResponse.class, PutEventsResponseEntry.class,
                TeaModel.class, TeaRequest.class, TeaResponse.class, TeaPair.class,};
        for (Class<?> clazz : aliyunClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
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
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        }
        registry.resources().add(NativeResourcesEntry.of("project.properties"));
        registry.resources().add(NativeResourcesEntry.of("endpoints.json"));
        registry.resources().add(NativeResourcesEntry.of("regions.txt"));
        // aliyun MNS
        final Class<?>[] mnsClassArray = {MNSClient.class, DefaultMNSClient.class};
        for (Class<?> clazz : mnsClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        }
        registry.resources().add(NativeResourcesEntry.of("versioninfo.properties"));
        registry.resources().add(NativeResourcesEntry.ofBundle("common"));
        //gson
        final Class<?>[] gsonClassArray = {Gson.class, GsonBuilder.class, TypeToken.class, JsonElement.class};
        for (Class<?> clazz : gsonClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        }
        //fastjson
        final Class<?>[] fastjsonClassArray = {JSONArray.class, JSONObject.class};
        for (Class<?> clazz : fastjsonClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        }
        //rocketmq
        final Class<?>[] rocketmqClassArray = {SendMessageRequestHeaderV2.class, SendMessageResponseHeader.class,
                UnregisterClientRequestHeader.class, GetRouteInfoRequestHeader.class, BrokerData.class, QueueData.class,
                TopicRouteData.class, NettyDecoder.class, NettyEncoder.class,
                LanguageCode.class, RemotingCommand.class, RemotingSerializable.class, SerializeType.class};
        for (Class<?> clazz : rocketmqClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        }
        //eclipse paho mqtt client
        registry.resources().add(NativeResourcesEntry.of("org/eclipse/paho/mqttv5/logging/JSR47Logger.class"));
        registry.resources().add(NativeResourcesEntry.of("org/eclipse/paho/mqttv5/client/internal/nls/logcat.properties"));
        registry.resources().add(NativeResourcesEntry.of("org/eclipse/paho/mqttv5/common/nls/logcat.properties"));
        registry.resources().add(NativeResourcesEntry.ofBundle("org/eclipse/paho/mqttv5/common/nls/messages"));
        final Class<?>[] mqttClassArray = {Logger.class, JSR47Logger.class, LoggerFactory.class,
                NetworkModuleFactory.class, WebSocketNetworkModuleFactory.class,
                WebSocketSecureNetworkModuleFactory.class, SSLNetworkModuleFactory.class,
                TCPNetworkModuleFactory.class};
        for (Class<?> clazz : mqttClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
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
                ByteBufPair.Encoder.class,
                org.apache.pulsar.common.protocol.PulsarDecoder.class,
                io.netty.channel.socket.nio.NioDatagramChannel.class,
                io.netty.channel.socket.nio.NioSocketChannel.class
        };
        for (Class<?> clazz : pulsarClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        }
    }
}
