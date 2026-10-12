package co.com.andesexpress.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import co.com.andesexpress.model.CatalogUnavailableException;

/** Prueba el adaptador contra un servidor HTTP local (JDK), sin tocar internet. */
class GeoCatalogAdapterTest {

    private static final String PREFIX = "/api/v1/City/";

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger hits = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(PREFIX, exchange -> {
            hits.incrementAndGet();
            String id = exchange.getRequestURI().getPath().substring(PREFIX.length());
            switch (id) {
                case "12" -> reply(exchange, 200, "{\"id\":12,\"name\":\"Medellin\",\"departmentId\":2}");
                case "500" -> reply(exchange, 500, "{}");
                case "50" -> reply(exchange, 200, "{\"id\":50,\"name\":\"Sin departamento\"}");
                case "777" -> {
                    pause(800);
                    reply(exchange, 200, "{\"id\":777,\"departmentId\":2}");
                }
                default -> reply(exchange, 404, "");
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1";
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    @Test
    void existingCityReturnsItsDepartment() {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl, 1000);

        assertEquals(Optional.of(2), adapter.findDepartmentIdOfCity(12));
    }

    @Test
    void unknownCityReturnsEmpty() {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl, 1000);

        assertEquals(Optional.empty(), adapter.findDepartmentIdOfCity(99999999));
    }

    @Test
    void serverErrorIsUnavailableNotUnknown() {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl, 1000);

        assertThrows(CatalogUnavailableException.class, () -> adapter.findDepartmentIdOfCity(500));
    }

    @Test
    void bodyWithoutDepartmentIsUnavailable() {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl, 1000);

        assertThrows(CatalogUnavailableException.class, () -> adapter.findDepartmentIdOfCity(50));
    }

    @Test
    void slowResponseTimesOutAsUnavailable() {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl, 200);

        assertThrows(CatalogUnavailableException.class, () -> adapter.findDepartmentIdOfCity(777));
    }

    @Test
    void unreachableServerIsUnavailable() {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl, 1000);
        server.stop(0);

        assertThrows(CatalogUnavailableException.class, () -> adapter.findDepartmentIdOfCity(12));
    }

    @Test
    void successfulLookupsAreCached() {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl, 1000);

        adapter.findDepartmentIdOfCity(12);
        adapter.findDepartmentIdOfCity(12);

        assertEquals(1, hits.get());
    }

    @Test
    void notFoundIsNotCached() {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl, 1000);

        adapter.findDepartmentIdOfCity(99999999);
        adapter.findDepartmentIdOfCity(99999999);

        assertEquals(2, hits.get());
    }

    @Test
    void trailingSlashInBaseUrlIsAccepted() {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl + "/", 1000);

        assertEquals(Optional.of(2), adapter.findDepartmentIdOfCity(12));
    }

    @Test
    void warmUpRunsOnceInTheBackgroundAndLeavesTheCityCached() throws InterruptedException {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl, 1000);

        adapter.warmUpInBackground();
        adapter.warmUpInBackground();
        long deadline = System.currentTimeMillis() + 3000;
        while (hits.get() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        Thread.sleep(200); // margen para detectar una segunda consulta que no deberia existir

        assertEquals(1, hits.get());
        adapter.findDepartmentIdOfCity(12);
        assertEquals(1, hits.get());
    }

    @Test
    void failedWarmUpDoesNotBreakTheAdapter() throws InterruptedException {
        GeoCatalogAdapter adapter = new GeoCatalogAdapter(baseUrl, 300);
        server.stop(0);

        adapter.warmUpInBackground();
        Thread.sleep(400);

        assertThrows(CatalogUnavailableException.class, () -> adapter.findDepartmentIdOfCity(12));
    }
}
