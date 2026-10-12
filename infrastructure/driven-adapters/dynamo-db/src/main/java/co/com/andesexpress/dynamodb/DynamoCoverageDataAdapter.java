package co.com.andesexpress.dynamodb;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import co.com.andesexpress.model.CatalogUnavailableException;
import co.com.andesexpress.model.Tariff;
import co.com.andesexpress.model.Zone;
import co.com.andesexpress.model.gateways.CoverageDataGateway;

/**
 * Lee vecinos y tarifas desde la tabla de configuracion.
 *
 * <ul>
 *   <li>Cache en memoria con vencimiento: una validacion no hace viajes a DynamoDB en cada peticion,
 *       y un cambio de tarifa en la tabla se aplica solo cuando vence la cache.</li>
 *   <li>Si DynamoDB falla y hay un dato vencido en cache, se sigue sirviendo ese dato.</li>
 *   <li>Un departamento ausente en la tabla es un error (503), no "sin vecinos": si no, un departamento
 *       sin cargar se clasificaria en silencio como REST_OF_COUNTRY.</li>
 * </ul>
 */
public class DynamoCoverageDataAdapter implements CoverageDataGateway {

    private final CoverageConfigStore store;
    private final long ttlNanos;
    private final LongSupplier nanoClock;
    private final Map<String, CachedItem> cache = new ConcurrentHashMap<>();

    public DynamoCoverageDataAdapter(CoverageConfigStore store, Duration cacheTtl) {
        this(store, cacheTtl, System::nanoTime);
    }

    DynamoCoverageDataAdapter(CoverageConfigStore store, Duration cacheTtl, LongSupplier nanoClock) {
        this.store = store;
        this.ttlNanos = cacheTtl.toNanos();
        this.nanoClock = nanoClock;
    }

    @Override
    public Set<Integer> neighborsOf(int departmentId) {
        CoverageConfigItem item = load(CoverageConfigItem.departmentKey(departmentId))
                .orElseThrow(() -> new CatalogUnavailableException(
                        "Faltan datos de cobertura: el departamento " + departmentId
                                + " no esta cargado en DynamoDB"));
        return item.getNeighbors() == null ? Set.of() : Set.copyOf(item.getNeighbors());
    }

    @Override
    public Optional<Tariff> findTariff(Zone zone) {
        return load(CoverageConfigItem.tariffKey(zone))
                .map(item -> new Tariff(zone, item.getBase(), item.getPerKg()));
    }

    private Optional<CoverageConfigItem> load(String pk) {
        long now = nanoClock.getAsLong();
        CachedItem cached = cache.get(pk);
        if (cached != null && now - cached.expiresAt() < 0) {
            return Optional.of(cached.item());
        }
        try {
            Optional<CoverageConfigItem> fresh = store.find(pk);
            if (fresh.isPresent()) {
                cache.put(pk, new CachedItem(fresh.get(), now + ttlNanos));
            } else {
                cache.remove(pk);
            }
            return fresh;
        } catch (CatalogUnavailableException e) {
            if (cached != null) {
                return Optional.of(cached.item());
            }
            throw e;
        }
    }

    private record CachedItem(CoverageConfigItem item, long expiresAt) {
    }
}
