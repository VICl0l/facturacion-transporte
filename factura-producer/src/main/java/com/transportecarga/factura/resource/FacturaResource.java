package com.transportecarga.factura.resource;

import com.transportecarga.factura.dto.FacturaCreadaRequest;
import com.transportecarga.factura.dto.PublicacionResponse;
import com.transportecarga.factura.service.FacturaProducerService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.CompletionException;

@Path("/api/facturas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FacturaResource {

    private static final Logger LOG =
            Logger.getLogger(FacturaResource.class);

    @Inject
    FacturaProducerService producerService;

    @POST
    @RunOnVirtualThread
    @RolesAllowed("facturador")
    public Response publicar(
            @NotNull @Valid FacturaCreadaRequest request
    ) {
        try {
            var metadata = producerService.publicar(request)
                    .toCompletableFuture()
                    .join();

            var respuesta = new PublicacionResponse(
                    request.facturaId(),
                    "Evento de factura publicado en Kafka",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );

            return Response.ok(respuesta).build();

        } catch (CompletionException error) {
            var causa = error.getCause();

            if (causa instanceof BadRequestException validacion) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "mensaje", validacion.getMessage()
                        ))
                        .build();
            }

            LOG.error("Error al publicar el evento de factura", causa);

            return Response.serverError()
                    .entity(Map.of(
                            "mensaje",
                            "No se pudo confirmar la publicación del evento"
                    ))
                    .build();
        }
    }
}