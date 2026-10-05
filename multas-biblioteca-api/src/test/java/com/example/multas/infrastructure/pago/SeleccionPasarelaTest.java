package com.example.multas.infrastructure.pago;

import com.example.multas.domain.port.PasarelaPagoPort;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// Cambiar app.pagos.proveedor cambia el adaptador sin tocar código: solo existe un bean del puerto
class SeleccionPasarelaTest {

    @Nested
    @SpringBootTest(properties = "app.pagos.proveedor=pagosudes")
    class ConPagosUdes {
        @Autowired
        ApplicationContext context;

        @Test
        void soloExistePagosUdesAdapter() {
            assertEquals(1, context.getBeansOfType(PasarelaPagoPort.class).size());
            assertInstanceOf(PagosUdesAdapter.class, context.getBean(PasarelaPagoPort.class));
        }
    }

    @Nested
    @SpringBootTest(properties = "app.pagos.proveedor=wompi")
    class ConWompi {
        @Autowired
        ApplicationContext context;

        @Test
        void soloExisteWompiAdapter() {
            assertEquals(1, context.getBeansOfType(PasarelaPagoPort.class).size());
            assertInstanceOf(WompiAdapter.class, context.getBean(PasarelaPagoPort.class));
        }
    }
}
