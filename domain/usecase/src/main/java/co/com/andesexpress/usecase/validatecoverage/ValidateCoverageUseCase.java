package co.com.andesexpress.usecase.validatecoverage;

import java.util.Optional;
import java.util.Set;

import co.com.andesexpress.model.CoverageResult;
import co.com.andesexpress.model.Location;
import co.com.andesexpress.model.RejectionReason;
import co.com.andesexpress.model.Tariff;
import co.com.andesexpress.model.TariffNotFoundException;
import co.com.andesexpress.model.Weight;
import co.com.andesexpress.model.Zone;
import co.com.andesexpress.model.ZoneClassifier;
import co.com.andesexpress.model.gateways.CoverageDataGateway;
import co.com.andesexpress.model.gateways.GeoCatalogGateway;

public class ValidateCoverageUseCase {

    private final GeoCatalogGateway geoCatalog;
    private final CoverageDataGateway coverageData;

    public ValidateCoverageUseCase(GeoCatalogGateway geoCatalog, CoverageDataGateway coverageData) {
        this.geoCatalog = geoCatalog;
        this.coverageData = coverageData;
    }

    /**
     * Rechazos de negocio -> CoverageResult.Invalid (el origen se revisa primero).
     * Fallas tecnicas -> excepcion (CatalogUnavailableException), nunca un Invalid.
     */
    public CoverageResult execute(Location origin, Location destination, Weight weight) {
        CoverageResult.Invalid problem = check(origin, true);
        if (problem == null) {
            problem = check(destination, false);
        }
        if (problem != null) {
            return problem;
        }

        Set<Integer> neighbors = coverageData.neighborsOf(origin.departmentId());
        Zone zone = ZoneClassifier.classify(origin, destination, neighbors);

        Tariff tariff = coverageData.findTariff(zone)
                .orElseThrow(() -> new TariffNotFoundException(zone));

        return new CoverageResult.Valid(zone, tariff.calculate(weight));
    }

    private CoverageResult.Invalid check(Location location, boolean isOrigin) {
        String side = isOrigin ? "origen" : "destino";
        Optional<Integer> department = geoCatalog.findDepartmentIdOfCity(location.cityId());

        if (department.isEmpty()) {
            return new CoverageResult.Invalid(
                    isOrigin ? RejectionReason.ORIGIN_CITY_NOT_FOUND : RejectionReason.DESTINATION_CITY_NOT_FOUND,
                    "El municipio de " + side + " con id " + location.cityId() + " no existe");
        }
        if (department.get() != location.departmentId()) {
            return new CoverageResult.Invalid(
                    isOrigin ? RejectionReason.ORIGIN_CITY_NOT_IN_DEPARTMENT
                            : RejectionReason.DESTINATION_CITY_NOT_IN_DEPARTMENT,
                    "El municipio de " + side + " con id " + location.cityId()
                            + " no pertenece al departamento con id " + location.departmentId());
        }
        return null;
    }
}
