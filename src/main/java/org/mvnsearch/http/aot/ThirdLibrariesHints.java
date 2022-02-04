package org.mvnsearch.http.aot;

import com.aliyun.eventbridge.models.CloudEvent;
import com.aliyun.eventbridge.models.PutEventsResponse;
import com.aliyun.eventbridge.models.PutEventsResponseEntry;
import com.aliyun.tea.TeaModel;
import com.aliyun.tea.TeaPair;
import com.aliyun.tea.TeaRequest;
import com.aliyun.tea.TeaResponse;
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
                TeaModel.class, TeaRequest.class, TeaResponse.class, TeaPair.class, };
        for (Class<?> clazz : aliyunClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        }
        //gson
        final Class<?>[] gsonClassArray = {Gson.class, GsonBuilder.class, TypeToken.class, JsonElement.class};
        for (Class<?> clazz : gsonClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        }
    }
}
