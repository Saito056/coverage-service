package co.com.andesexpress.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LocationTest {

    @Test
    void validLocationKeepsItsIds() {
        Location location = new Location(2, 12);

        assertEquals(2, location.departmentId());
        assertEquals(12, location.cityId());
    }

    @Test
    void smallestValidIdsAreAccepted() {
        new Location(1, 1);
    }

    @Test
    void zeroDepartmentIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Location(0, 1));
    }

    @Test
    void zeroCityIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Location(1, 0));
    }

    @Test
    void negativeIdsAreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Location(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Location(1, -1));
    }
}
