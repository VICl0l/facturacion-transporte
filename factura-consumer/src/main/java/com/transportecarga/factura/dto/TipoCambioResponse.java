package com.transportecarga.factura.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TipoCambioResponse(
        LocalDate date,
        String base,
        String quote,
        BigDecimal rate
) {
}