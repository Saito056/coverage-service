package co.com.andesexpress.model;

/** Resultado de negocio de validar cobertura: o es valido (con zona y tarifa) o se rechaza con un motivo. */
public sealed interface CoverageResult {

    record Valid(Zone zone, Fare fare) implements CoverageResult {
    }

    record Invalid(RejectionReason reason, String message) implements CoverageResult {
    }
}
