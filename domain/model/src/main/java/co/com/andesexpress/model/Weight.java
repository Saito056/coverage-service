package co.com.andesexpress.model;

import java.math.BigDecimal;

public record Weight(BigDecimal kg) {

    public Weight {
        if (kg == null || kg.signum() <= 0) {
            throw new IllegalArgumentException(
                    "El peso debe ser mayor que cero"
            );
        }
    }
}