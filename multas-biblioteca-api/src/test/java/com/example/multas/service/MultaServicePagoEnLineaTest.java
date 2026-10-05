package com.example.multas.service;

import com.example.multas.domain.PagoRechazadoException;
import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.SolicitudPago;
import com.example.multas.domain.port.PasarelaPagoPort;
import com.example.multas.model.EstadoMulta;
import com.example.multas.model.Multa;
import com.example.multas.model.MultaYaPagadaException;
import com.example.multas.repository.MultaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// MultaService probado con una pasarela falsa: no necesita Spring, HTTP ni saber cuál pasarela es
class MultaServicePagoEnLineaTest {

    private MultaRepository repository;
    private Multa multa;
    private final List<SolicitudPago> solicitudesRecibidas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = mock(MultaRepository.class);
        multa = new Multa();
        multa.setId(7L);
        multa.setEstudianteId("U00123");
        multa.setMonto(new BigDecimal("2000"));
        when(repository.findById(7L)).thenReturn(Optional.of(multa));
        when(repository.save(any(Multa.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PasarelaPagoPort pasarelaQueResponde(ResultadoPago resultado) {
        return solicitud -> {
            solicitudesRecibidas.add(solicitud);
            return resultado;
        };
    }

    @Test
    void pagoAprobadoMarcaLaMultaConElProveedor() {
        MultaService service = new MultaService(repository,
                pasarelaQueResponde(new ResultadoPago("WOMPI", true, "multa-7", "ok")));

        Multa pagada = service.pagarConPasarela(7L);

        assertEquals(EstadoMulta.PAGADA, pagada.getEstado());
        assertEquals("WOMPI", pagada.getMetodoPago());
        assertEquals(new SolicitudPago(7L, "U00123", new BigDecimal("2000")), solicitudesRecibidas.get(0));
    }

    @Test
    void pagoRechazadoLanzaExcepcionConElMensajeDelProveedorYNoGuarda() {
        MultaService service = new MultaService(repository,
                pasarelaQueResponde(new ResultadoPago("PAGOSUDES", false, "PU-1", "Transaccion rechazada por PagosUDES")));

        PagoRechazadoException ex = assertThrows(PagoRechazadoException.class, () -> service.pagarConPasarela(7L));

        assertEquals("Transaccion rechazada por PagosUDES", ex.getMessage());
        assertEquals(EstadoMulta.PENDIENTE, multa.getEstado());
        verify(repository, never()).save(any());
    }

    @Test
    void multaYaPagadaNoLlegaALaPasarela() {
        multa.marcarComoPagada("VENTANILLA");
        MultaService service = new MultaService(repository,
                pasarelaQueResponde(new ResultadoPago("PAGOSUDES", true, "PU-1", "ok")));

        assertThrows(MultaYaPagadaException.class, () -> service.pagarConPasarela(7L));
        assertTrue(solicitudesRecibidas.isEmpty());
    }
}
