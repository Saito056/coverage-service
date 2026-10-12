package co.com.andesexpress.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Tariff(
        Zone zone,
        BigDecimal base,
        BigDecimal perKg
) {

    public Fare calculate(Weight weight) {

        BigDecimal amount = base
                .add(perKg.multiply(weight.kg()))
                .setScale(0, RoundingMode.HALF_UP);

        return new Fare(amount, "COP");
    }
}