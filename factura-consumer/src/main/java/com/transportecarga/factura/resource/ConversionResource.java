package com.transportecarga.factura.resource;

import com.transportecarga.factura.dto.ConversionResponse;
import com.transportecarga.factura.service.ConversionService;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/facturas/{facturaId}/conversion")
@Produces(MediaType.APPLICATION_JSON)
public class ConversionResource {

    @Inject
    ConversionService conversionService;

    @GET
    @Blocking
    @RolesAllowed("auditor")
    public ConversionResponse convertir(
            @PathParam("facturaId") UUID facturaId,
            @QueryParam("monedaDestino") String monedaDestino
    ) {
        return conversionService.convertir(
                facturaId,
                monedaDestino
        );
    }
}