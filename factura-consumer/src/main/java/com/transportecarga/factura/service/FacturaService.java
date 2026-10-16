package com.transportecarga.factura.service;

import com.transportecarga.factura.avro.FacturaCreada;
import com.transportecarga.factura.mapper.FacturaMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FacturaService {

    @Inject
    FacturaMapper mapper;

    @Inject
    EntityManager entityManager;

    @Transactional
    public boolean guardar(FacturaCreada evento) {
        var factura = mapper.toEntity(evento);

        var sql = """
                INSERT INTO facturas (
                    factura_id,
                    evento_id,
                    version_evento,
                    ocurrido_en,
                    numero_factura,
                    fecha_emision,
                    cliente_id,
                    cliente_nombre,
                    servicio_transporte_id,
                    origen,
                    destino,
                    descripcion_carga,
                    moneda,
                    subtotal,
                    impuestos,
                    total
                )
                VALUES (
                    :facturaId,
                    :eventoId,
                    :versionEvento,
                    :ocurridoEn,
                    :numeroFactura,
                    :fechaEmision,
                    :clienteId,
                    :clienteNombre,
                    :servicioTransporteId,
                    :origen,
                    :destino,
                    :descripcionCarga,
                    :moneda,
                    :subtotal,
                    :impuestos,
                    :total
                )
                ON CONFLICT (factura_id) DO NOTHING
                """;

        int filasInsertadas = entityManager.createNativeQuery(sql)
                .setParameter("facturaId", factura.facturaId)
                .setParameter("eventoId", factura.eventoId)
                .setParameter("versionEvento", factura.versionEvento)
                .setParameter("ocurridoEn", factura.ocurridoEn)
                .setParameter("numeroFactura", factura.numeroFactura)
                .setParameter("fechaEmision", factura.fechaEmision)
                .setParameter("clienteId", factura.clienteId)
                .setParameter("clienteNombre", factura.clienteNombre)
                .setParameter(
                        "servicioTransporteId",
                        factura.servicioTransporteId
                )
                .setParameter("origen", factura.origen)
                .setParameter("destino", factura.destino)
                .setParameter("descripcionCarga", factura.descripcionCarga)
                .setParameter("moneda", factura.moneda)
                .setParameter("subtotal", factura.subtotal)
                .setParameter("impuestos", factura.impuestos)
                .setParameter("total", factura.total)
                .executeUpdate();

        return filasInsertadas == 1;
    }
}