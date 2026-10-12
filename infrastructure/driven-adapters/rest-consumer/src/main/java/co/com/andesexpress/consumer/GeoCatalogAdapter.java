package co.com.andesexpress.consumer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.com.andesexpress.model.CatalogUnavailableException;
import co.com.andesexpress.model.gateways.GeoCatalogGateway;

/**
 * Adaptador de salida hacia API Colombia (GET {baseUrl}/City/{id}).
 *
 * - 200 con departmentId  -> el municipio existe y se conoce su departamento.
 * - 404                   -> el municipio no existe (Optional.empty()).
 * - cualquier otra cosa   -> CatalogUnavailableException (nunca se confunde con "no existe").
 *
 * Tiempos: una consulta con la conexion en frio (DNS + TCP + TLS + JVM sin calentar) puede tardar
 * bastante mas que una con la conexion ya abierta, por eso el timeout por consulta es holgado
 * (2,5 s por defecto). Esto no rompe el presupuesto de Orders: si una consulta supera los 3 s de
 * Orders, igual termina y queda en cache, y el reintento de Orders (que espera 500 ms) ya la encuentra.
 * Este adaptador no reintenta por su cuenta.
 *
 * Al arrancar hace una consulta de calentamiento en segundo plano (DNS, TLS, JIT) para que la
 * primera peticion real no pague ese costo.
 */
@Component
public class GeoCatalogAdapter implements GeoCatalogGateway {

    private static final System.Logger LOG = System.getLogger(GeoCatalogAdapter.class.getName());
    /** Medellin: el municipio concreto da igual, solo se busca abrir la conexion y calentar la JVM. */
    private static final int WARM_UP_CITY_ID = 12;

    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    // municipio -> departamento. Solo se guardan consultas exitosas; esa relacion no cambia.
    private final Map<Integer, Integer> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean warmedUp = new AtomicBoolean();

    public GeoCatalogAdapter(@Value("${adapter.restconsumer.url}") String baseUrl,
                             @Value("${adapter.restconsumer.timeout-ms:2500}") long timeoutMs) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = Duration.ofMillis(timeoutMs);
        // HTTP/1.1: evita los cuelgues de conexiones HTTP/2 reutilizadas que se quedan viejas
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void warmUpInBackground() {
        if (!warmedUp.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                findDepartmentIdOfCity(WARM_UP_CITY_ID);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.DEBUG, "Calentamiento de API Colombia sin exito (no es grave)", e);
            }
        }, "geo-catalog-warm-up");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public Optional<Integer> findDepartmentIdOfCity(int cityId) {
        Integer cached = cache.get(cityId);
        if (cached != null) {
            return Optional.of(cached);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/City/" + cityId))
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("User-Agent", "andesexpress-coverage/1.0")
                .GET()
                .build();
        long startedAt = System.nanoTime();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 404) {
                return Optional.empty();
            }
            if (status != 200) {
                throw new CatalogUnavailableException("API Colombia respondio HTTP " + status);
            }

            JsonNode department = mapper.readTree(response.body()).path("departmentId");
            if (!department.isInt()) {
                throw new CatalogUnavailableException(
                        "Respuesta inesperada de API Colombia para el municipio " + cityId);
            }
            cache.put(cityId, department.asInt());
            return Optional.of(department.asInt());
        } catch (IOException e) {
            // incluye timeouts (HttpTimeoutException), conexion rechazada, DNS y JSON ilegible
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            LOG.log(System.Logger.Level.WARNING, "API Colombia: fallo consultando el municipio " + cityId
                    + " tras " + elapsedMs + " ms (" + e.getClass().getSimpleName() + ")", e);
            throw new CatalogUnavailableException("No se pudo consultar API Colombia", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CatalogUnavailableException("Consulta a API Colombia interrumpida", e);
        }
    }
}
