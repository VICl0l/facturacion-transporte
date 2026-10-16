package com.transportecarga.factura.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FacturaCreadaRequest(

        @NotNull
        UUID facturaId,

        @NotBlank
        String numeroFactura,

        @NotNull
        LocalDate fechaEmision,

        @NotBlank
        String clienteId,

        @NotBlank
        String clienteNombre,

        @NotBlank
        String servicioTransporteId,

        @NotBlank
        String origen,

        @NotBlank
        String destino,

        @NotBlank
        String descripcionCarga,

        @NotBlank
        @Pattern(
                regexp = "[A-Z]{3}",
                message = "debe contener tres letras mayúsculas"
        )
        String moneda,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 12, fraction = 2)
        BigDecimal subtotal,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 12, fraction = 2)
        BigDecimal impuestos,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 12, fraction = 2)
        BigDecimal total

) {
}