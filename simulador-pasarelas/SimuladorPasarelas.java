import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simulador de las dos pasarelas para probar el pago en linea sin servicios reales.
 * Solo usa el JDK. Ejecutar con: java SimuladorPasarelas.java
 *
 * - PagosUDES (puerto 9001): POST /pagosudes/transacciones
 *   recibe {estudianteId, monto} y responde {idTransaccion, estadoTransaccion: APROBADA | RECHAZADA}
 * - Wompi (puerto 9002): POST /wompi/transactions
 *   recibe {reference, amountInCents} y responde {reference, status: APPROVED | DECLINED}
 *
 * Regla del simulador: los pagos de mas de $10.000 se rechazan (simula fondos insuficientes).
 */
public class SimuladorPasarelas {

    private static final BigDecimal LIMITE_PESOS = new BigDecimal("10000");

    public static void main(String[] args) throws IOException {
        HttpServer pagosUdes = HttpServer.create(new InetSocketAddress(9001), 0);
        pagosUdes.createContext("/pagosudes/transacciones", SimuladorPasarelas::pagosUdes);
        pagosUdes.start();

        HttpServer wompi = HttpServer.create(new InetSocketAddress(9002), 0);
        wompi.createContext("/wompi/transactions", SimuladorPasarelas::wompi);
        wompi.start();

        System.out.println("PagosUDES simulado en http://localhost:9001/pagosudes/transacciones");
        System.out.println("Wompi simulado en     http://localhost:9002/wompi/transactions");
    }

    private static void pagosUdes(HttpExchange exchange) throws IOException {
        String body = leer(exchange);
        BigDecimal monto = new BigDecimal(campo(body, "monto"));
        String estado = monto.compareTo(LIMITE_PESOS) > 0 ? "RECHAZADA" : "APROBADA";
        String id = "PU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("[PagosUDES] recibido " + body + " -> " + estado);
        responder(exchange, "{\"idTransaccion\":\"" + id + "\",\"estadoTransaccion\":\"" + estado + "\"}");
    }

    private static void wompi(HttpExchange exchange) throws IOException {
        String body = leer(exchange);
        long centavos = Long.parseLong(campo(body, "amountInCents"));
        String status = centavos > LIMITE_PESOS.longValue() * 100 ? "DECLINED" : "APPROVED";
        String reference = campo(body, "reference");
        System.out.println("[Wompi] recibido " + body + " -> " + status);
        responder(exchange, "{\"reference\":\"" + reference + "\",\"status\":\"" + status + "\"}");
    }

    private static String leer(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    // Lee un campo simple del JSON (texto o numero) sin librerias externas
    private static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*\"?([^\",}]+)\"?").matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Falta el campo " + nombre);
        }
        return m.group(1).trim();
    }

    private static void responder(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
