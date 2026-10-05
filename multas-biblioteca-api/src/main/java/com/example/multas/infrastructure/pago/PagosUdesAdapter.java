package com.example.multas.infrastructure.pago;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.SolicitudPago;
import com.example.multas.domain.port.PasarelaPagoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(prefix = "app.pagos", name = "proveedor", havingValue = "pagosudes", matchIfMissing = true)
public class PagosUdesAdapter implements PasarelaPagoPort {

    private static final String PROVEEDOR = "PAGOSUDES";

    private final RestTemplate restTemplate;
    private final String urlPasarela;

    public PagosUdesAdapter(RestTemplate restTemplate,
                            @Value("${app.pagos.pagosudes.url:http://localhost:9001/pagosudes/transacciones}") String urlPasarela) {
        this.restTemplate = restTemplate;
        this.urlPasarela = urlPasarela;
    }

    @Override
    public ResultadoPago procesar(SolicitudPago solicitud) {
        try {
            PagosUdesRequest request = new PagosUdesRequest(solicitud.estudianteId(), solicitud.monto());
            PagosUdesResponse response = restTemplate.postForObject(urlPasarela, request, PagosUdesResponse.class);
            boolean exitoso = response != null && "APROBADA".equals(response.estadoTransaccion());
            return new ResultadoPago(PROVEEDOR, exitoso,
                    response != null ? response.idTransaccion() : null,
                    exitoso ? "Pago aprobado por PagosUDES" : "Transaccion rechazada por PagosUDES");
        } catch (RestClientException ex) {
            return new ResultadoPago(PROVEEDOR, false, null, "PagosUDES no disponible: " + ex.getMessage());
        }
    }

    // Formato propio de PagosUDES: no sale de esta clase
    record PagosUdesRequest(String estudianteId, BigDecimal monto) {
    }

    record PagosUdesResponse(String idTransaccion, String estadoTransaccion) {
    }
}
