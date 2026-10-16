package com.transportecarga.factura.service;

import com.transportecarga.factura.dto.FacturaCreadaRequest;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.CompletionStage;

public interface FacturaProducerService {

    CompletionStage<RecordMetadata> publicar(
            FacturaCreadaRequest request
    );

}