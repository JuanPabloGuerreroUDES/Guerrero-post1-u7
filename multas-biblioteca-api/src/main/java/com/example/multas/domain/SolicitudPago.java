package com.example.multas.domain;

import java.math.BigDecimal;

// Lo único que una pasarela necesita saber de la multa. Se usa en vez de la entidad
// Multa para que el puerto no dependa de JPA.
public record SolicitudPago(
        Long multaId,
        String estudianteId,
        BigDecimal monto
) {
}
