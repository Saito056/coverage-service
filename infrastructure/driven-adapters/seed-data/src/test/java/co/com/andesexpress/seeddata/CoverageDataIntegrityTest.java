package co.com.andesexpress.seeddata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import co.com.andesexpress.model.Zone;

/**
 * Protege contra el fallo silencioso: un departamento ausente en coverage-data.json
 * se clasificaria como REST_OF_COUNTRY sin dar ningun error.
 */
class CoverageDataIntegrityTest {

    private static final int FIRST_DEPARTMENT_ID = 1;
    private static final int LAST_DEPARTMENT_ID = 33;
    private static final int SAN_ANDRES_ID = 27; // isla: unico departamento sin vecinos terrestres

    private final CoverageDataAdapter data = new CoverageDataAdapter();

    @Test
    void everyDepartmentOfApiColombiaIsDefined() {
        for (int id = FIRST_DEPARTMENT_ID; id <= LAST_DEPARTMENT_ID; id++) {
            if (id == SAN_ANDRES_ID) {
                assertTrue(data.neighborsOf(id).isEmpty(), "San Andres no tiene vecinos terrestres");
            } else {
                assertFalse(data.neighborsOf(id).isEmpty(), "Faltan vecinos del departamento " + id);
            }
        }
    }

    @Test
    void neighborsAreSymmetricAndNeverSelfReferencing() {
        for (int id = FIRST_DEPARTMENT_ID; id <= LAST_DEPARTMENT_ID; id++) {
            assertFalse(data.neighborsOf(id).contains(id), "El departamento " + id + " es vecino de si mismo");
            for (int neighbor : data.neighborsOf(id)) {
                assertTrue(data.neighborsOf(neighbor).contains(id),
                        "Asimetria: " + id + " lista a " + neighbor + " pero no al reves");
            }
        }
    }

    @Test
    void everyZoneHasATariff() {
        for (Zone zone : Zone.values()) {
            assertTrue(data.findTariff(zone).isPresent(), "Falta la tarifa de " + zone);
        }
    }
}
