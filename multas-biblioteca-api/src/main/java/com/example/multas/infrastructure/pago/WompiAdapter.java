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
@ConditionalOnProperty(prefix = "app.pagos", name = "proveedor", havingValue = "wompi")
public class WompiAdapter implements PasarelaPagoPort {

    private static final String PROVEEDOR = "WOMPI";

    private final RestTemplate restTemplate;
    private final String urlPasarela;

    public WompiAdapter(RestTemplate restTemplate,
                        @Value("${app.pagos.wompi.url:http://localhost:9002/wompi/transactions}") String urlPasarela) {
        this.restTemplate = restTemplate;
        this.urlPasarela = urlPasarela;
    }

    @Override
    public ResultadoPago procesar(SolicitudPago solicitud) {
        try {
            // Wompi trabaja en centavos
            long montoEnCentavos = solicitud.monto().multiply(BigDecimal.valueOf(100)).longValueExact();
            WompiRequest request = new WompiRequest("multa-" + solicitud.multaId(), montoEnCentavos);
            WompiResponse response = restTemplate.postForObject(urlPasarela, request, WompiResponse.class);
            boolean exitoso = response != null && "APPROVED".equalsIgnoreCase(response.status());
            return new ResultadoPago(PROVEEDOR, exitoso,
                    response != null ? response.reference() : null,
                    exitoso ? "Pago aprobado por Wompi" : "Pago rechazado por Wompi");
        } catch (RestClientException ex) {
            return new ResultadoPago(PROVEEDOR, false, null, "Wompi no disponible: " + ex.getMessage());
        }
    }

    // Formato propio de Wompi: no sale de esta clase
    record WompiRequest(String reference, long amountInCents) {
    }

    record WompiResponse(String reference, String status) {
    }
}
