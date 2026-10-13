package co.com.andesexpress.dynamodb;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import co.com.andesexpress.model.CatalogUnavailableException;

/** Tabla falsa en memoria: cuenta lecturas y puede simular que DynamoDB esta caido. */
class InMemoryConfigStore implements CoverageConfigStore {

    final Map<String, CoverageConfigItem> items = new HashMap<>();
    int finds;
    boolean failing;

    @Override
    public Optional<CoverageConfigItem> find(String pk) {
        finds++;
        if (failing) {
            throw new CatalogUnavailableException("DynamoDB caido (simulado)");
        }
        return Optional.ofNullable(items.get(pk));
    }

    @Override
    public void save(CoverageConfigItem item) {
        if (failing) {
            throw new CatalogUnavailableException("DynamoDB caido (simulado)");
        }
        items.put(item.getPk(), item);
    }
}
