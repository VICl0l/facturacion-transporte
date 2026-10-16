package com.transportecarga.factura.config;

import com.transportecarga.factura.avro.FacturaCreada;
import com.transportecarga.factura.properties.KafkaValues;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Properties;

@Singleton
public class KafkaProducerConfig {

    @Produces
    @Singleton
    @Named("facturaProducer")
    public Producer<String, FacturaCreada> createProducer(
            KafkaValues kafkaValues
    ) {
        var properties = new Properties();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaValues.bootstrapServers()
        );

        properties.put(
                ProducerConfig.CLIENT_ID_CONFIG,
                "factura-producer"
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName()
        );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaAvroSerializer.class.getName()
        );

        properties.put(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                kafkaValues.schemaRegistryUrl()
        );

        properties.put(
                AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS,
                false
        );

        properties.put(
                AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION,
                false
        );

        properties.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        properties.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true
        );

        properties.put("avro.remove.java.properties", true);

        return new KafkaProducer<>(properties);
    }

    public void closeProducer(
            @Disposes
            @Named("facturaProducer")
            Producer<String, FacturaCreada> producer
    ) {
        producer.close(Duration.ofSeconds(10));
    }
}