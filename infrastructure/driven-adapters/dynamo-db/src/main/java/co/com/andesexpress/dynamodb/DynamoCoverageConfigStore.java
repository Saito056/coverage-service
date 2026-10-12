package co.com.andesexpress.dynamodb;

import java.util.Optional;

import co.com.andesexpress.model.CatalogUnavailableException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/** Implementacion real sobre DynamoDB. Solo usa GetItem y PutItem (nunca Scan). */
public class DynamoCoverageConfigStore implements CoverageConfigStore {

    private static final System.Logger LOG = System.getLogger(DynamoCoverageConfigStore.class.getName());

    private final DynamoDbTable<CoverageConfigItem> table;
    private final String tableName;

    public DynamoCoverageConfigStore(DynamoDbEnhancedClient client, String tableName) {
        this.tableName = tableName;
        this.table = client.table(tableName, TableSchema.fromBean(CoverageConfigItem.class));
    }

    @Override
    public Optional<CoverageConfigItem> find(String pk) {
        try {
            return Optional.ofNullable(table.getItem(Key.builder().partitionValue(pk).build()));
        } catch (SdkException e) {
            throw unavailable("leer", pk, e);
        }
    }

    @Override
    public void save(CoverageConfigItem item) {
        try {
            table.putItem(item);
        } catch (SdkException e) {
            throw unavailable("escribir", item.getPk(), e);
        }
    }

    private CatalogUnavailableException unavailable(String action, String pk, SdkException cause) {
        LOG.log(System.Logger.Level.WARNING,
                "DynamoDB: no se pudo " + action + " " + pk + " en la tabla " + tableName, cause);
        return new CatalogUnavailableException(
                "No se pudo acceder a la configuracion de cobertura en DynamoDB", cause);
    }
}
