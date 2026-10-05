package com.example.multas.controller;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.port.PasarelaPagoPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PagoEnLineaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasarelaPagoPort pasarela;

    private void crearMulta() throws Exception {
        mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estudianteId\":\"U001\",\"concepto\":\"Devolucion tardia\",\"diasAtraso\":3}"))
                .andExpect(status().isCreated());
    }

    @Test
    void pagoAprobadoDevuelve200ConElProveedorComoMetodo() throws Exception {
        when(pasarela.procesar(any())).thenReturn(new ResultadoPago("PAGOSUDES", true, "PU-1", "ok"));
        crearMulta();

        mockMvc.perform(post("/api/multas/1/pagar-en-linea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.metodoPago").value("PAGOSUDES"));
    }

    @Test
    void pagoRechazadoDevuelve402() throws Exception {
        when(pasarela.procesar(any())).thenReturn(new ResultadoPago("WOMPI", false, "multa-1", "Pago rechazado por Wompi"));
        crearMulta();

        mockMvc.perform(post("/api/multas/1/pagar-en-linea"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("Pago rechazado por Wompi"));
    }

    @Test
    void multaPagadaEnVentanillaDevuelve409AlPagarEnLinea() throws Exception {
        crearMulta();
        mockMvc.perform(patch("/api/multas/1/pagar")).andExpect(status().isOk());

        mockMvc.perform(post("/api/multas/1/pagar-en-linea"))
                .andExpect(status().isConflict());
    }

    @Test
    void multaPagadaEnLineaDevuelve409AlRepetir() throws Exception {
        when(pasarela.procesar(any())).thenReturn(new ResultadoPago("PAGOSUDES", true, "PU-1", "ok"));
        crearMulta();
        mockMvc.perform(post("/api/multas/1/pagar-en-linea")).andExpect(status().isOk());

        mockMvc.perform(post("/api/multas/1/pagar-en-linea"))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/multas/1/pagar"))
                .andExpect(status().isConflict());
    }
}
