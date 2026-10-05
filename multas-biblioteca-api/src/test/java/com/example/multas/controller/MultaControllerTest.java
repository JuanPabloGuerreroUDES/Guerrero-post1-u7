package com.example.multas.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class MultaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String json(String estudianteId, int dias) {
        return """
                {"estudianteId": "%s", "concepto": "Devolucion tardia", "diasAtraso": %d}
                """.formatted(estudianteId, dias);
    }

    @Test
    void alInicioLaListaEstaVacia() throws Exception {
        mockMvc.perform(get("/api/multas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void generarMultaDevuelve201ConElMontoCalculado() throws Exception {
        mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON).content(json("U001", 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monto").value(2000))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void sinEstudianteIdDevuelve400() throws Exception {
        mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\": \"Devolucion tardia\", \"diasAtraso\": 2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estudianteId").exists());
    }

    @Test
    void laCuartaMultaPendienteDevuelve409() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON).content(json("U002", 1)))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON).content(json("U002", 1)))
                .andExpect(status().isConflict());
    }

    @Test
    void idInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/multas/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pagarEnVentanillaYRepetirDevuelve409() throws Exception {
        mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON).content(json("U003", 2)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/multas/1/pagar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.metodoPago").value("VENTANILLA"));

        mockMvc.perform(patch("/api/multas/1/pagar"))
                .andExpect(status().isConflict());
    }
}
