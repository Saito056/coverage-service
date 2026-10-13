package co.com.andesexpress.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.com.andesexpress.model.CatalogUnavailableException;
import co.com.andesexpress.model.Tariff;
import co.com.andesexpress.model.Zone;

class DynamoCoverageDataAdapterTest {

    private static final Duration TTL = Duration.ofSeconds(60);

    private InMemoryConfigStore store;
    private AtomicLong nanos;
    private DynamoCoverageDataAdapter adapter;

    @BeforeEach
    void setUp() {
        store = new InMemoryConfigStore();
        nanos = new AtomicLong(1_000);
        adapter = new DynamoCoverageDataAdapter(store, TTL, nanos::get);
        store.save(CoverageConfigItem.department(2, Set.of(6, 7, 8, 13, 14, 26, 28)));
        store.save(CoverageConfigItem.department(27, Set.of()));
        store.save(CoverageConfigItem.tariff(Zone.NEIGHBORING_DEPARTMENT,
                new BigDecimal("14000"), new BigDecimal("1200")));
    }

    private void advance(Duration d) {
        nanos.addAndGet(d.toNanos());
    }

    @Test
    void neighborsOfReturnsTheStoredNeighbors() {
        assertEquals(Set.of(6, 7, 8, 13, 14, 26, 28), adapter.neighborsOf(2));
    }

    @Test
    void departmentWithoutLandNeighborsReturnsEmptySet() {
        assertEquals(Set.of(), adapter.neighborsOf(27));
    }

    @Test
    void departmentMissingFromTableIsUnavailableNotEmpty() {
        assertThrows(CatalogUnavailableException.class, () -> adapter.neighborsOf(5));
    }

    @Test
    void findTariffBuildsTheDomainTariff() {
        Optional<Tariff> tariff = adapter.findTariff(Zone.NEIGHBORING_DEPARTMENT);

        assertEquals(Optional.of(new Tariff(Zone.NEIGHBORING_DEPARTMENT,
                new BigDecimal("14000"), new BigDecimal("1200"))), tariff);
    }

    @Test
    void missingTariffIsEmpty() {
        assertEquals(Optional.empty(), adapter.findTariff(Zone.SAME_CITY));
    }

    @Test
    void secondReadIsServedFromCache() {
        adapter.neighborsOf(2);
        adapter.neighborsOf(2);

        assertEquals(1, store.finds);
    }

    @Test
    void expiredEntryIsReadAgain() {
        adapter.neighborsOf(2);
        advance(TTL.plusSeconds(1));
        adapter.neighborsOf(2);

        assertEquals(2, store.finds);
    }

    @Test
    void editedTariffIsPickedUpAfterTheCacheExpires() {
        adapter.findTariff(Zone.NEIGHBORING_DEPARTMENT);
        store.save(CoverageConfigItem.tariff(Zone.NEIGHBORING_DEPARTMENT,
                new BigDecimal("15000"), new BigDecimal("1300")));
        advance(TTL.plusSeconds(1));

        assertEquals(new BigDecimal("15000"), adapter.findTariff(Zone.NEIGHBORING_DEPARTMENT).orElseThrow().base());
    }

    @Test
    void missingItemIsNotCached() {
        adapter.findTariff(Zone.SAME_CITY);
        adapter.findTariff(Zone.SAME_CITY);

        assertEquals(2, store.finds);
    }

    @Test
    void staleValueIsServedWhenDynamoFailsAfterExpiry() {
        adapter.neighborsOf(2);
        advance(TTL.plusSeconds(1));
        store.failing = true;

        assertEquals(Set.of(6, 7, 8, 13, 14, 26, 28), adapter.neighborsOf(2));
    }

    @Test
    void failureWithNothingCachedPropagates() {
        store.failing = true;

        assertThrows(CatalogUnavailableException.class, () -> adapter.neighborsOf(2));
    }

    @Test
    void neighborsAreStoredSortedAsAList() {
        CoverageConfigItem item = CoverageConfigItem.department(9, Set.of(30, 4, 17));

        assertEquals(List.of(4, 17, 30), item.getNeighbors());
        assertTrue(item.getPk().equals("DEPARTMENT#9"));
    }
}
