package co.com.andesexpress.dynamodb;

import java.util.Optional;

import co.com.andesexpress.model.Tariff;
import co.com.andesexpress.model.Zone;
import co.com.andesexpress.model.gateways.CoverageDataGateway;

/**
 * Llena la tabla con los datos de una fuente (el JSON) SOLO donde falta algo: es idempotente
 * y nunca pisa una tarifa que alguien haya editado en la tabla.
 */
public class DynamoCoverageDataSeeder {

    /** IDs de departamento de API Colombia. */
    static final int FIRST_DEPARTMENT_ID = 1;
    static final int LAST_DEPARTMENT_ID = 33;

    private final CoverageConfigStore store;

    public DynamoCoverageDataSeeder(CoverageConfigStore store) {
        this.store = store;
    }

    /** @return cuantos items se escribieron */
    public int seedMissing(CoverageDataGateway source) {
        int written = 0;
        for (int id = FIRST_DEPARTMENT_ID; id <= LAST_DEPARTMENT_ID; id++) {
            if (store.find(CoverageConfigItem.departmentKey(id)).isEmpty()) {
                store.save(CoverageConfigItem.department(id, source.neighborsOf(id)));
                written++;
            }
        }
        for (Zone zone : Zone.values()) {
            if (store.find(CoverageConfigItem.tariffKey(zone)).isEmpty()) {
                Optional<Tariff> tariff = source.findTariff(zone);
                if (tariff.isPresent()) {
                    store.save(CoverageConfigItem.tariff(zone, tariff.get().base(), tariff.get().perKg()));
                    written++;
                }
            }
        }
        return written;
    }
}
