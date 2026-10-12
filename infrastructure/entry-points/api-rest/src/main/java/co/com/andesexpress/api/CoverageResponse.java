package co.com.andesexpress.api;

import java.math.BigDecimal;

/**
 * Respuesta HTTP 200 de POST /coverage/validate. Dos formas, segun el contrato:
 *   valido:   { "valid": true,  "zone": "...", "fare": { "amount": ..., "currency": "COP" } }
 *   invalido: { "valid": false, "reason": "...", "message": "..." }
 */
public sealed interface CoverageResponse {

    record Valid(boolean valid, String zone, FareBody fare) implements CoverageResponse {
    }

    record Invalid(boolean valid, String reason, String message) implements CoverageResponse {
    }

    record FareBody(BigDecimal amount, String currency) {
    }
}
