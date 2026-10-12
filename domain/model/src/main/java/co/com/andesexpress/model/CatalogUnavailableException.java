package co.com.andesexpress.model;

/**
 * El catalogo geografico (API Colombia) no pudo consultarse: timeout, red caida,
 * respuesta HTTP inesperada, etc. NO significa que la ubicacion no exista.
 */
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message) {
        super(message);
    }

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
