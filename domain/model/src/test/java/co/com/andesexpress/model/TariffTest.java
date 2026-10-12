package co.com.andesexpress.model;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TariffTest {

    @Test
    void neighborFareFor10_5Kg() {

        Tariff tariff = new Tariff(
                Zone.NEIGHBORING_DEPARTMENT,
                new BigDecimal("14000"),
                new BigDecimal("1200")
        );
        Fare fare = tariff.calculate(
                new Weight(new BigDecimal("10.5"))
        );

        assertEquals(
                new BigDecimal("26600"),
                fare.amount()
        );
    }

    @Test
    void restOfCountryFareFor10_5Kg() {

        Tariff tariff = new Tariff(
                Zone.REST_OF_COUNTRY,
                new BigDecimal("20000"),
                new BigDecimal("1800")
        );
        Fare fare = tariff.calculate(
                new Weight(new BigDecimal("10.5"))
        );

        assertEquals(
                new BigDecimal("38900"),
                fare.amount()
        );
    }

    @Test
    void sameCityFareFor3Kg() {

        Tariff tariff = new Tariff(
                Zone.SAME_CITY,
                new BigDecimal("6000"),
                new BigDecimal("500")
        );
        Fare fare = tariff.calculate(
                new Weight(new BigDecimal("3"))
        );

        assertEquals(
                new BigDecimal("7500"),
                fare.amount()
        );
    }

    @Test
    void roundsCorrectly() {

        Tariff tariff = new Tariff(
                Zone.SAME_CITY,
                new BigDecimal("6000"),
                new BigDecimal("500")
        );
         Fare fare = tariff.calculate(
                new Weight(new BigDecimal("0.001"))
        );

        assertEquals(
                new BigDecimal("6001"),
                fare.amount()
        );
    }
}