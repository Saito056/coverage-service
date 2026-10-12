package co.com.andesexpress.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WeightTest {

    @Test
    void zeroWeightIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Weight(BigDecimal.ZERO)
        );
    }
    @Test
    void negativeWeightIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Weight(new BigDecimal("-1"))
        );
    }

    @Test
    void nullWeightIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Weight(null)
        );
    }
}
