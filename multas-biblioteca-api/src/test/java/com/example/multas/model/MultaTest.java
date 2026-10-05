package com.example.multas.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultaTest {

    @Test
    void calculaQuinientosPorCadaDiaDeAtraso() {
        assertEquals(new BigDecimal("500"), Multa.calcularMonto(1));
        assertEquals(new BigDecimal("4000"), Multa.calcularMonto(8));
    }

    @Test
    void elMontoNoPasaDelTope() {
        assertEquals(new BigDecimal("15000"), Multa.calcularMonto(30));
        assertEquals(new BigDecimal("15000"), Multa.calcularMonto(90));
    }

    @Test
    void marcarComoPagadaDosVecesLanzaExcepcion() {
        Multa multa = new Multa();
        multa.marcarComoPagada("VENTANILLA");

        assertEquals(EstadoMulta.PAGADA, multa.getEstado());
        assertEquals("VENTANILLA", multa.getMetodoPago());
        assertThrows(MultaYaPagadaException.class, () -> multa.marcarComoPagada("VENTANILLA"));
    }
}
