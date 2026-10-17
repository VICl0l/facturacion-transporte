package com.transportecarga.factura.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ConversionResponse(
        UUID facturaId,
        String monedaOriginal,
        BigDecimal totalOriginal,
        String monedaDestino,
        BigDecimal tasa,
        LocalDate fechaTasa,
        BigDecimal totalConvertido,
        String fuente
) {
}