package com.transportecarga.factura.dto;

import java.util.UUID;

public record PublicacionResponse(
        UUID facturaId,
        String mensaje,
        String topic,
        int partition,
        long offset
) {
}