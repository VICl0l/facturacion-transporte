package com.transportecarga.factura.service;

import com.transportecarga.factura.client.TipoCambioClient;
import com.transportecarga.factura.dto.ConversionResponse;
import com.transportecarga.factura.dto.TipoCambioResponse;
import com.transportecarga.factura.entity.Factura;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ConversionService {

    private static final Logger LOG =
            Logger.getLogger(ConversionService.class);

    @Inject
    @RestClient
    TipoCambioClient client;

    public ConversionResponse convertir(UUID facturaId, String destino) {
        if (destino == null || !destino.matches("[A-Za-z]{3}")) {
            throw new BadRequestException(
                    "monedaDestino debe contener tres letras"
            );
        }

        String monedaDestino = destino.toUpperCase(Locale.ROOT);

        Factura factura = Factura.findById(facturaId);

        if (factura == null) {
            throw new NotFoundException("Factura no encontrada");
        }

        String monedaOriginal = factura.moneda.toUpperCase(Locale.ROOT);

        if (monedaOriginal.equals(monedaDestino)) {
            throw new BadRequestException(
                    "La moneda destino debe ser distinta de la original"
            );
        }

        TipoCambioResponse cambio = consultarTasa(
                monedaOriginal,
                monedaDestino
        );

        if (cambio == null
                || cambio.date() == null
                || cambio.rate() == null
                || cambio.rate().compareTo(BigDecimal.ZERO) <= 0
                || !monedaOriginal.equalsIgnoreCase(cambio.base())
                || !monedaDestino.equalsIgnoreCase(cambio.quote())) {
            throw errorExterno(
                    502,
                    "La API externa devolvió una tasa inválida"
            );
        }

        BigDecimal convertido = factura.total
                .multiply(cambio.rate())
                .setScale(2, RoundingMode.HALF_UP);

        return new ConversionResponse(
                factura.facturaId,
                monedaOriginal,
                factura.total,
                monedaDestino,
                cambio.rate(),
                cambio.date(),
                convertido,
                "Frankfurter"
        );
    }

    private TipoCambioResponse consultarTasa(
            String base,
            String destino
    ) {
        try {
            return client.consultar(
                    base.toLowerCase(Locale.ROOT),
                    destino.toLowerCase(Locale.ROOT)
            );
        } catch (WebApplicationException error) {
            int estado = error.getResponse().getStatus();

            if (estado == 400 || estado == 404 || estado == 422) {
                throw new BadRequestException(
                        "El par de monedas no está disponible"
                );
            }

            LOG.error("La API de cambio rechazó la consulta", error);

            throw errorExterno(
                    502,
                    "No se pudo obtener la tasa externa"
            );
        } catch (ProcessingException error) {
            LOG.error("Error de comunicación con la API de cambio", error);

            if (esTimeout(error)) {
                throw errorExterno(
                        504,
                        "La API externa excedió el tiempo de espera"
                );
            }

            throw errorExterno(
                    502,
                    "No se pudo consultar la API externa"
            );
        }
    }

    private boolean esTimeout(Throwable error) {
        for (Throwable causa = error;
             causa != null;
             causa = causa.getCause()) {

            if (causa.getClass().getSimpleName()
                    .endsWith("TimeoutException")) {
                return true;
            }
        }

        return false;
    }

    private WebApplicationException errorExterno(
            int estado,
            String mensaje
    ) {
        return new WebApplicationException(
                Response.status(estado)
                        .entity(Map.of("mensaje", mensaje))
                        .build()
        );
    }
}