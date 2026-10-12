package co.com.andesexpress.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import co.com.andesexpress.model.CatalogUnavailableException;
import co.com.andesexpress.model.TariffNotFoundException;

/**
 * Un destino/origen invalido NO pasa por aqui: es un resultado de negocio y responde 200 con valid=false.
 * Aqui solo viven los errores: 400 (request mal formado), 503 (catalogo no disponible) y 500 (config rota).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, message(e));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadableBody(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido o le faltan campos");
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    public ResponseEntity<Map<String, String>> catalogUnavailable(CatalogUnavailableException e) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, message(e));
    }

    @ExceptionHandler(TariffNotFoundException.class)
    public ResponseEntity<Map<String, String>> noTariff(TariffNotFoundException e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, message(e));
    }

    private ResponseEntity<Map<String, String>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    // Map.of lanza NullPointerException si el mensaje es null
    private String message(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
