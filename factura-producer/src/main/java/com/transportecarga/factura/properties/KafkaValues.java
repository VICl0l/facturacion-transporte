package com.transportecarga.factura.properties;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "application.kafka")
public interface KafkaValues {

    String bootstrapServers();

    String topic();

    String schemaRegistryUrl();
}