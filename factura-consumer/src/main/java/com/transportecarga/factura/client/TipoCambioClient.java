package com.transportecarga.factura.client;

import com.transportecarga.factura.dto.TipoCambioResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/v2")
@RegisterRestClient(configKey = "tipo-cambio")
@Produces(MediaType.APPLICATION_JSON)
public interface TipoCambioClient {

    @GET
    @Path("/rate/{base}/{destino}")
    TipoCambioResponse consultar(
            @PathParam("base") String base,
            @PathParam("destino") String destino
    );
}