package com.example.multas.infrastructure.pago;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.SolicitudPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Cada adaptador habla un formato distinto pero los dos devuelven el mismo ResultadoPago
class AdaptadoresPagoTest {

    private static final String URL_PAGOSUDES = "http://pagosudes.test/transacciones";
    private static final String URL_WOMPI = "http://wompi.test/transactions";

    private RestTemplate restTemplate;
    private MockRestServiceServer servidor;
    private final SolicitudPago solicitud = new SolicitudPago(5L, "U00123", new BigDecimal("2000.00"));

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void pagosUdesAprobadaSeTraduceAResultadoExitoso() {
        servidor.expect(requestTo(URL_PAGOSUDES))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"estudianteId\":\"U00123\",\"monto\":2000.00}"))
                .andRespond(withSuccess("{\"idTransaccion\":\"PU-123\",\"estadoTransaccion\":\"APROBADA\"}",
                        MediaType.APPLICATION_JSON));

        ResultadoPago resultado = new PagosUdesAdapter(restTemplate, URL_PAGOSUDES).procesar(solicitud);

        assertEquals(new ResultadoPago("PAGOSUDES", true, "PU-123", "Pago aprobado por PagosUDES"), resultado);
        servidor.verify();
    }

    @Test
    void pagosUdesRechazadaSeTraduceAResultadoFallido() {
        servidor.expect(requestTo(URL_PAGOSUDES))
                .andRespond(withSuccess("{\"idTransaccion\":\"PU-124\",\"estadoTransaccion\":\"RECHAZADA\"}",
                        MediaType.APPLICATION_JSON));

        ResultadoPago resultado = new PagosUdesAdapter(restTemplate, URL_PAGOSUDES).procesar(solicitud);

        assertFalse(resultado.exitoso());
        assertEquals("Transaccion rechazada por PagosUDES", resultado.mensaje());
    }

    @Test
    void pagosUdesCaidaNoRevientaSinoQueDevuelveFallido() {
        servidor.expect(requestTo(URL_PAGOSUDES)).andRespond(withServerError());

        ResultadoPago resultado = new PagosUdesAdapter(restTemplate, URL_PAGOSUDES).procesar(solicitud);

        assertFalse(resultado.exitoso());
        assertTrue(resultado.mensaje().startsWith("PagosUDES no disponible"));
    }

    @Test
    void wompiEnviaCentavosYTraduceApproved() {
        servidor.expect(requestTo(URL_WOMPI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"reference\":\"multa-5\",\"amountInCents\":200000}"))
                .andRespond(withSuccess("{\"reference\":\"multa-5\",\"status\":\"APPROVED\"}",
                        MediaType.APPLICATION_JSON));

        ResultadoPago resultado = new WompiAdapter(restTemplate, URL_WOMPI).procesar(solicitud);

        assertEquals(new ResultadoPago("WOMPI", true, "multa-5", "Pago aprobado por Wompi"), resultado);
        servidor.verify();
    }

    @Test
    void wompiDeclinedSeTraduceAResultadoFallido() {
        servidor.expect(requestTo(URL_WOMPI))
                .andRespond(withSuccess("{\"reference\":\"multa-5\",\"status\":\"DECLINED\"}",
                        MediaType.APPLICATION_JSON));

        ResultadoPago resultado = new WompiAdapter(restTemplate, URL_WOMPI).procesar(solicitud);

        assertFalse(resultado.exitoso());
        assertEquals("Pago rechazado por Wompi", resultado.mensaje());
    }
}
