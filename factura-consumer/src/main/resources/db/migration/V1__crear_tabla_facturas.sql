CREATE TABLE facturas (
      factura_id UUID PRIMARY KEY,
      evento_id UUID NOT NULL UNIQUE,
      version_evento INTEGER NOT NULL,
      ocurrido_en TIMESTAMP WITH TIME ZONE NOT NULL,

      numero_factura VARCHAR(100) NOT NULL,
      fecha_emision DATE NOT NULL,

      cliente_id VARCHAR(100) NOT NULL,
      cliente_nombre TEXT NOT NULL,

      servicio_transporte_id VARCHAR(100) NOT NULL,
      origen TEXT NOT NULL,
      destino TEXT NOT NULL,
      descripcion_carga TEXT NOT NULL,

      moneda VARCHAR(3) NOT NULL,
      subtotal NUMERIC(14, 2) NOT NULL,
      impuestos NUMERIC(14, 2) NOT NULL,
      total NUMERIC(14, 2) NOT NULL,

      registrado_en TIMESTAMP WITH TIME ZONE
          NOT NULL DEFAULT CURRENT_TIMESTAMP,

      CONSTRAINT ck_facturas_importes
          CHECK (
              subtotal >= 0
                  AND impuestos >= 0
                  AND total > 0
                  AND total = subtotal + impuestos
              )
);