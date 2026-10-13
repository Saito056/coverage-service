package co.com.andesexpress.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.com.andesexpress.model.CatalogUnavailableException;
import co.com.andesexpress.model.Tariff;
import co.com.andesexpress.model.Zone;
import co.com.andesexpress.model.gateways.CoverageDataGateway;

class DynamoCoverageDataSeederTest {

    private static final int DEPARTMENTS = 33;
    private static final int TARIFFS = Zone.values().length;

    private final CoverageDataGateway source = new CoverageDataGateway() {
        @Override
        public Set<Integer> neighborsOf(int departmentId) {
            return departmentId == 27 ? Set.of() : Set.of(8, 6, 7);
        }

        @Override
        public Optional<Tariff> findTariff(Zone zone) {
            return Optional.of(new Tariff(zone, new BigDecimal("1000"), new BigDecimal("100")));
        }
    };

    private InMemoryConfigStore store;
    private DynamoCoverageDataSeeder seeder;

    @BeforeEach
    void setUp() {
        store = new InMemoryConfigStore();
        seeder = new DynamoCoverageDataSeeder(store);
    }

    @Test
    void loadsEveryDepartmentAndEveryTariffIntoAnEmptyTable() {
        int written = seeder.seedMissing(source);

        assertEquals(DEPARTMENTS + TARIFFS, written);
        assertEquals(DEPARTMENTS + TARIFFS, store.items.size());
    }

    @Test
    void secondRunWritesNothing() {
        seeder.seedMissing(source);

        assertEquals(0, seeder.seedMissing(source));
    }

    @Test
    void neverOverwritesAnEditedTariff() {
        store.save(CoverageConfigItem.tariff(Zone.SAME_CITY, new BigDecimal("1"), new BigDecimal("2")));

        int written = seeder.seedMissing(source);

        assertEquals(DEPARTMENTS + TARIFFS - 1, written);
        assertEquals(new BigDecimal("1"), store.items.get(CoverageConfigItem.tariffKey(Zone.SAME_CITY)).getBase());
    }

    @Test
    void storesNeighborsSortedAndKeepsTheIslandWithAnEmptyList() {
        seeder.seedMissing(source);

        assertEquals(List.of(6, 7, 8), store.items.get(CoverageConfigItem.departmentKey(2)).getNeighbors());
        assertEquals(List.of(), store.items.get(CoverageConfigItem.departmentKey(27)).getNeighbors());
    }

    @Test
    void failsLoudlyWhenDynamoIsDown() {
        store.failing = true;

        assertThrows(CatalogUnavailableException.class, () -> seeder.seedMissing(source));
    }
}
