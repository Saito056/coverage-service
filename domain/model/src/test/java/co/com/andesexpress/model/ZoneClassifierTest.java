package co.com.andesexpress.model;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoneClassifierTest {

    @Test
    void sameCity() {
        Zone zone = ZoneClassifier.classify(
                new Location(2, 12),
                new Location(2, 12),
                Set.of()
        );

        assertEquals(Zone.SAME_CITY, zone);
    }
    @Test
    void sameDepartment() {
        Zone zone = ZoneClassifier.classify(
                new Location(2, 12),
                new Location(2, 50),
                Set.of()
        );

        assertEquals(Zone.SAME_DEPARTMENT, zone);
    }

    @Test
    void medellinToManizalesIsNeighbor() {
        Zone zone = ZoneClassifier.classify(
                new Location(2, 12),
                new Location(8, 337),
                Set.of(6, 7, 8, 13, 14, 26, 28)
        );
         assertEquals(
                Zone.NEIGHBORING_DEPARTMENT,
                zone
        );
    }

    @Test
    void medellinToBogotaIsRestOfCountry() {
        Zone zone = ZoneClassifier.classify(
                new Location(2, 12),
                new Location(5, 167),
                Set.of(6, 7, 8, 13, 14, 26, 28)
        );

        assertEquals(
                Zone.REST_OF_COUNTRY,
                zone
        );
    }

    @Test
    void bogotaToCundinamarcaIsNeighbor() {
        Zone zone = ZoneClassifier.classify(
                new Location(5, 167),
                new Location(15, 1),
                Set.of(15, 21)
        );

        assertEquals(
                Zone.NEIGHBORING_DEPARTMENT,
                zone
        );
    }

     @Test
    void islandWithoutNeighborsIsRestOfCountry() {
        Zone zone = ZoneClassifier.classify(
                new Location(27, 912),
                new Location(2, 12),
                Set.of()
        );

        assertEquals(
                Zone.REST_OF_COUNTRY,
                zone
        );
    }
}