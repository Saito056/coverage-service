package co.com.andesexpress.model.gateways;

import java.util.Optional;

import co.com.andesexpress.model.CatalogUnavailableException;

public interface GeoCatalogGateway {

    /**
     * @return el id del departamento al que pertenece el municipio, o vacio si el municipio no existe.
     * @throws CatalogUnavailableException si no fue posible consultar el catalogo (no es lo mismo que "no existe").
     */
    Optional<Integer> findDepartmentIdOfCity(int cityId);
}
