package com.transportecarga.factura.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "facturas")
public class Factura extends PanacheEntityBase {

    @Id
    @Column(name = "factura_id", nullable = false)
    public UUID facturaId;

    @Column(name = "evento_id", nullable = false, unique = true)
    public UUID eventoId;

    @Column(name = "version_evento", nullable = false)
    public int versionEvento;

    @Column(name = "ocurrido_en", nullable = false)
    public Instant ocurridoEn;

    @Column(name = "numero_factura", nullable = false, length = 100)
    public String numeroFactura;

    @Column(name = "fecha_emision", nullable = false)
    public LocalDate fechaEmision;

    @Column(name = "cliente_id", nullable = false, length = 100)
    public String clienteId;

    @Column(name = "cliente_nombre", nullable = false, columnDefinition = "text")
    public String clienteNombre;

    @Column(name = "servicio_transporte_id", nullable = false, length = 100)
    public String servicioTransporteId;

    @Column(name = "origen", nullable = false, columnDefinition = "text")
    public String origen;

    @Column(name = "destino", nullable = false, columnDefinition = "text")
    public String destino;

    @Column(name = "descripcion_carga", nullable = false, columnDefinition = "text")
    public String descripcionCarga;

    @Column(name = "moneda", nullable = false, length = 3)
    public String moneda;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    public BigDecimal subtotal;

    @Column(name = "impuestos", nullable = false, precision = 14, scale = 2)
    public BigDecimal impuestos;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    public BigDecimal total;

    @Column(
            name = "registrado_en",
            nullable = false,
            insertable = false,
            updatable = false
    )
    public Instant registradoEn;
}