package com.transportecarga.factura.mapper;

import com.transportecarga.factura.avro.FacturaCreada;
import com.transportecarga.factura.entity.Factura;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@ApplicationScoped
public class FacturaMapper {

    public Factura toEntity(FacturaCreada evento) {
        validarTipoYVersion(evento);

        var factura = new Factura();

        factura.facturaId = UUID.fromString(
                evento.getFacturaId().toString()
        );

        factura.eventoId = UUID.fromString(
                evento.getEventoId().toString()
        );

        factura.versionEvento = evento.getVersionEvento();

        factura.ocurridoEn = Instant.parse(
                evento.getOcurridoEn().toString()
        );

        factura.numeroFactura = evento.getNumeroFactura().toString();

        factura.fechaEmision = LocalDate.parse(
                evento.getFechaEmision().toString()
        );

        factura.clienteId = evento.getClienteId().toString();
        factura.clienteNombre = evento.getClienteNombre().toString();

        factura.servicioTransporteId =
                evento.getServicioTransporteId().toString();

        factura.origen = evento.getOrigen().toString();
        factura.destino = evento.getDestino().toString();
        factura.descripcionCarga =
                evento.getDescripcionCarga().toString();

        factura.moneda = evento.getMoneda().toString();
        factura.subtotal = evento.getSubtotal();
        factura.impuestos = evento.getImpuestos();
        factura.total = evento.getTotal();

        return factura;
    }

    private void validarTipoYVersion(FacturaCreada evento) {
        if (!"FacturaCreada".equals(evento.getTipoEvento().toString())) {
            throw new IllegalArgumentException(
                    "Tipo de evento no soportado: " + evento.getTipoEvento()
            );
        }

        if (evento.getVersionEvento() != 1) {
            throw new IllegalArgumentException(
                    "Versión de evento no soportada: "
                            + evento.getVersionEvento()
            );
        }
    }
}