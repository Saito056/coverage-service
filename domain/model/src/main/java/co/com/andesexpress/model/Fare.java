package co.com.andesexpress.model;

import java.math.BigDecimal;

public record Fare(BigDecimal amount, String currency) {
}