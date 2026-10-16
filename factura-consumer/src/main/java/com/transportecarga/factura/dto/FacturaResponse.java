package com.transportecarga.factura.dto;

import com.transportecarga.factura.entity.Factura;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FacturaResponse(
        UUID facturaId,
        UUID eventoId,
        String numeroFactura,
        LocalDate fechaEmision,
        String clienteId,
        String clienteNombre,
        String servicioTransporteId,
        String origen,
        String destino,
        String descripcionCarga,
        String moneda,
        BigDecimal subtotal,
        BigDecimal impuestos,
        BigDecimal total,
        Instant registradoEn
) {

    public static FacturaResponse from(Factura factura) {
        return new FacturaResponse(
                factura.facturaId,
                factura.eventoId,
                factura.numeroFactura,
                factura.fechaEmision,
                factura.clienteId,
                factura.clienteNombre,
                factura.servicioTransporteId,
                factura.origen,
                factura.destino,
                factura.descripcionCarga,
                factura.moneda,
                factura.subtotal,
                factura.impuestos,
                factura.total,
                factura.registradoEn
        );
    }
}