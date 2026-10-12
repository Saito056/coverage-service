package co.com.andesexpress.dynamodb;

import java.util.Optional;

/**
 * Acceso minimo a la tabla de configuracion. Existe para que el adaptador y el cargador
 * se puedan probar sin DynamoDB (con una implementacion en memoria).
 */
public interface CoverageConfigStore {

    /** @throws co.com.andesexpress.model.CatalogUnavailableException si DynamoDB no responde */
    Optional<CoverageConfigItem> find(String pk);

    /** @throws co.com.andesexpress.model.CatalogUnavailableException si DynamoDB no responde */
    void save(CoverageConfigItem item);
}
