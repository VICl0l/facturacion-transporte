package com.transportecarga.factura.resource;

import com.transportecarga.factura.dto.FacturaResponse;
import com.transportecarga.factura.entity.Factura;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/facturas")
@Produces(MediaType.APPLICATION_JSON)
public class FacturaResource {

    @GET
    @Path("/{facturaId}")
    @Blocking
    @RolesAllowed("auditor")
    public FacturaResponse buscar(
            @PathParam("facturaId") UUID facturaId
    ) {
        Factura factura = Factura.findById(facturaId);

        if (factura == null) {
            throw new NotFoundException("Factura no encontrada");
        }

        return FacturaResponse.from(factura);
    }
}