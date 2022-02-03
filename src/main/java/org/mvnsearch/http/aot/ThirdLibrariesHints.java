package org.mvnsearch.http.aot;

import com.sun.mail.util.MailSSLSocketFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
                LongDeserializer.class, LongSerializer.class, StringDeserializer.class, StringSerializer.class};
        for (Class<?> clazz : kafkaClassArray) {
            registry.reflection().forType(clazz).withAccess(TypeAccess.DECLARED_CONSTRUCTORS)
                    .withAccess(TypeAccess.DECLARED_METHODS).withAccess(TypeAccess.DECLARED_FIELDS).build();
        }
        registry.resources().add(NativeResourcesEntry.of("kafka/kafka-version.properties"));
    }
}
