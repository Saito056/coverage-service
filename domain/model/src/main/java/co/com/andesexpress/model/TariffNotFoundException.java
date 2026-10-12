package co.com.andesexpress.model;

public class TariffNotFoundException extends RuntimeException {
    public TariffNotFoundException(Zone zone) {
        super("No hay tarifa configurada para la zona " + zone);
    }
}