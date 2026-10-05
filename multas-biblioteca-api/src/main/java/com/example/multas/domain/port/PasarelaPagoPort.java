package com.example.multas.domain.port;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.SolicitudPago;

// Puerto de salida: el dominio dice qué necesita de una pasarela, cada adaptador
// de infrastructure/pago/ decide cómo hablar con la suya.
public interface PasarelaPagoPort {

    ResultadoPago procesar(SolicitudPago solicitud);
}
