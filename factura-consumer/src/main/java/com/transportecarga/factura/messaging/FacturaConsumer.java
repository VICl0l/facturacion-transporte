package com.transportecarga.factura.messaging;

import com.transportecarga.factura.avro.FacturaCreada;
import com.transportecarga.factura.service.FacturaService;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class FacturaConsumer {

    private static final Logger LOG =
            Logger.getLogger(FacturaConsumer.class);

    @Inject
    FacturaService facturaService;

    @Incoming("facturas")
    @Blocking
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public void consumir(FacturaCreada evento) {
        boolean insertada = facturaService.guardar(evento);

        if (insertada) {
            LOG.infof(
                    "Factura guardada: facturaId=%s, eventoId=%s",
                    evento.getFacturaId(),
                    evento.getEventoId()
            );
        } else {
            LOG.infof(
                    "Factura existente, sin cambios: facturaId=%s",
                    evento.getFacturaId()
            );
        }
    }
}