package co.com.andesexpress.api;

import java.math.BigDecimal;

/** Request de POST /coverage/validate, tal como lo define el contrato Orders <-> Coverage. */
public record CoverageRequest(Place origin, Place destination, BigDecimal totalWeightKg) {

    /** IDs de API Colombia (no codigos DANE). */
    public record Place(int departmentId, int cityId) {
    }
}
