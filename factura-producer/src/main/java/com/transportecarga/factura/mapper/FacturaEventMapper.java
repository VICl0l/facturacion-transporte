package com.transportecarga.factura.mapper;

import com.transportecarga.factura.avro.FacturaCreada;
import com.transportecarga.factura.dto.FacturaCreadaRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class FacturaEventMapper {

    public FacturaCreada toEvent(FacturaCreadaRequest request) {
        validarImportes(request);

        return FacturaCreada.newBuilder()
                .setEventoId(UUID.randomUUID().toString())
                .setTipoEvento("FacturaCreada")
                .setVersionEvento(1)
                .setOcurridoEn(Instant.now().toString())
                .setFacturaId(request.facturaId().toString())
                .setNumeroFactura(request.numeroFactura())
                .setFechaEmision(request.fechaEmision().toString())
                .setClienteId(request.clienteId())
                .setClienteNombre(request.clienteNombre())
                .setServicioTransporteId(request.servicioTransporteId())
                .setOrigen(request.origen())
                .setDestino(request.destino())
                .setDescripcionCarga(request.descripcionCarga())
                .setMoneda(request.moneda())
                .setSubtotal(
                        request.subtotal().setScale(2, RoundingMode.UNNECESSARY)
                )
                .setImpuestos(
                        request.impuestos().setScale(2, RoundingMode.UNNECESSARY)
                )
                .setTotal(
                        request.total().setScale(2, RoundingMode.UNNECESSARY)
                )
                .build();
    }

    private void validarImportes(FacturaCreadaRequest request) {
        var totalCalculado = request.subtotal().add(request.impuestos());

        if (totalCalculado.compareTo(request.total()) != 0) {
            throw new BadRequestException(
                    "El total debe ser igual al subtotal más los impuestos"
            );
        }
    }
}