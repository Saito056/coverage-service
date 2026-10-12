package co.com.andesexpress.dynamodb;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import co.com.andesexpress.model.Zone;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Fila de la tabla de configuracion de Coverage (una sola tabla, clave de particion "pk"):
 *
 * <pre>
 * pk = "DEPARTMENT#2"                -> neighbors = [6, 7, 8, 13, 14, 26, 28]
 * pk = "TARIFF#NEIGHBORING_DEPARTMENT" -> base = 14000, perKg = 1200
 * </pre>
 *
 * Los vecinos se guardan como lista (L) y no como conjunto de numeros (NS) porque DynamoDB
 * no admite conjuntos vacios y San Andres no tiene vecinos terrestres.
 */
@DynamoDbBean
public class CoverageConfigItem {

    private String pk;
    private List<Integer> neighbors;
    private BigDecimal base;
    private BigDecimal perKg;

    public CoverageConfigItem() {
        // requerido por el mapeador de DynamoDB
    }

    public static String departmentKey(int departmentId) {
        return "DEPARTMENT#" + departmentId;
    }

    public static String tariffKey(Zone zone) {
        return "TARIFF#" + zone.name();
    }

    public static CoverageConfigItem department(int departmentId, Collection<Integer> neighbors) {
        CoverageConfigItem item = new CoverageConfigItem();
        item.pk = departmentKey(departmentId);
        item.neighbors = new ArrayList<>(new TreeSet<>(neighbors));
        return item;
    }

    public static CoverageConfigItem tariff(Zone zone, BigDecimal base, BigDecimal perKg) {
        CoverageConfigItem item = new CoverageConfigItem();
        item.pk = tariffKey(zone);
        item.base = base;
        item.perKg = perKg;
        return item;
    }

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public List<Integer> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(List<Integer> neighbors) {
        this.neighbors = neighbors;
    }

    public BigDecimal getBase() {
        return base;
    }

    public void setBase(BigDecimal base) {
        this.base = base;
    }

    public BigDecimal getPerKg() {
        return perKg;
    }

    public void setPerKg(BigDecimal perKg) {
        this.perKg = perKg;
    }
}
