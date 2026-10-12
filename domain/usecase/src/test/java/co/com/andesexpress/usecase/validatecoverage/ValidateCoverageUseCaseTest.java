package co.com.andesexpress.usecase.validatecoverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import co.com.andesexpress.model.CatalogUnavailableException;
import co.com.andesexpress.model.CoverageResult;
import co.com.andesexpress.model.Location;
import co.com.andesexpress.model.RejectionReason;
import co.com.andesexpress.model.Tariff;
import co.com.andesexpress.model.TariffNotFoundException;
import co.com.andesexpress.model.Weight;
import co.com.andesexpress.model.Zone;
import co.com.andesexpress.model.gateways.CoverageDataGateway;
import co.com.andesexpress.model.gateways.GeoCatalogGateway;

class ValidateCoverageUseCaseTest {

    // municipio -> departamento (ids reales de API Colombia)
    private static final Map<Integer, Integer> CITY_TO_DEPARTMENT = Map.of(
            12, 2,     // Medellin -> Antioquia
            337, 8,    // Manizales -> Caldas
            167, 5,    // Bogota D.C. -> Bogota
            912, 27);  // San Andres -> San Andres y Providencia

    private static final Map<Integer, Set<Integer>> NEIGHBORS = Map.of(
            2, Set.of(6, 7, 8, 13, 14, 26, 28),
            5, Set.of(15, 21),
            27, Set.of());

    private static final Map<Zone, Tariff> TARIFFS = Map.of(
            Zone.SAME_CITY, tariff(Zone.SAME_CITY, "6000", "500"),
            Zone.SAME_DEPARTMENT, tariff(Zone.SAME_DEPARTMENT, "9000", "800"),
            Zone.NEIGHBORING_DEPARTMENT, tariff(Zone.NEIGHBORING_DEPARTMENT, "14000", "1200"),
            Zone.REST_OF_COUNTRY, tariff(Zone.REST_OF_COUNTRY, "20000", "1800"));

    private final GeoCatalogGateway catalog = cityId -> Optional.ofNullable(CITY_TO_DEPARTMENT.get(cityId));

    private final CoverageDataGateway data = new CoverageDataGateway() {
        @Override
        public Set<Integer> neighborsOf(int departmentId) {
            return NEIGHBORS.getOrDefault(departmentId, Set.of());
        }

        @Override
        public Optional<Tariff> findTariff(Zone zone) {
            return Optional.ofNullable(TARIFFS.get(zone));
        }
    };

    private final ValidateCoverageUseCase useCase = new ValidateCoverageUseCase(catalog, data);

    private static Tariff tariff(Zone zone, String base, String perKg) {
        return new Tariff(zone, new BigDecimal(base), new BigDecimal(perKg));
    }

    private static Weight kg(String value) {
        return new Weight(new BigDecimal(value));
    }

    @Test
    void neighboringDepartmentIsValidWithItsFare() {
        CoverageResult result = useCase.execute(new Location(2, 12), new Location(8, 337), kg("10.5"));

        CoverageResult.Valid valid = (CoverageResult.Valid) result;
        assertEquals(Zone.NEIGHBORING_DEPARTMENT, valid.zone());
        assertEquals(new BigDecimal("26600"), valid.fare().amount());
        assertEquals("COP", valid.fare().currency());
    }

    @Test
    void farDepartmentIsRestOfCountry() {
        CoverageResult result = useCase.execute(new Location(2, 12), new Location(5, 167), kg("10.5"));

        CoverageResult.Valid valid = (CoverageResult.Valid) result;
        assertEquals(Zone.REST_OF_COUNTRY, valid.zone());
        assertEquals(new BigDecimal("38900"), valid.fare().amount());
    }

    @Test
    void sameCityIsSameCityZone() {
        CoverageResult result = useCase.execute(new Location(2, 12), new Location(2, 12), kg("3"));

        CoverageResult.Valid valid = (CoverageResult.Valid) result;
        assertEquals(Zone.SAME_CITY, valid.zone());
        assertEquals(new BigDecimal("7500"), valid.fare().amount());
    }

    @Test
    void unknownDestinationCityIsRejectedWithAClearMessage() {
        CoverageResult result = useCase.execute(new Location(2, 12), new Location(8, 99999), kg("1"));

        CoverageResult.Invalid invalid = (CoverageResult.Invalid) result;
        assertEquals(RejectionReason.DESTINATION_CITY_NOT_FOUND, invalid.reason());
        assertTrue(invalid.message().contains("destino"));
        assertTrue(invalid.message().contains("99999"));
    }

    @Test
    void cityThatBelongsToAnotherDepartmentIsRejected() {
        // 337 es Manizales (departamento 8), no de Antioquia (2)
        CoverageResult result = useCase.execute(new Location(2, 12), new Location(2, 337), kg("1"));

        CoverageResult.Invalid invalid = (CoverageResult.Invalid) result;
        assertEquals(RejectionReason.DESTINATION_CITY_NOT_IN_DEPARTMENT, invalid.reason());
        assertTrue(invalid.message().contains("337"));
        assertTrue(invalid.message().contains("departamento con id 2"));
    }

    @Test
    void invalidOriginIsReportedBeforeInvalidDestination() {
        CoverageResult result = useCase.execute(new Location(2, 99999), new Location(8, 99998), kg("1"));

        CoverageResult.Invalid invalid = (CoverageResult.Invalid) result;
        assertEquals(RejectionReason.ORIGIN_CITY_NOT_FOUND, invalid.reason());
    }

    @Test
    void originInWrongDepartmentIsRejected() {
        CoverageResult result = useCase.execute(new Location(5, 12), new Location(8, 337), kg("1"));

        CoverageResult.Invalid invalid = (CoverageResult.Invalid) result;
        assertEquals(RejectionReason.ORIGIN_CITY_NOT_IN_DEPARTMENT, invalid.reason());
    }

    @Test
    void islandWithoutNeighborsIsRestOfCountry() {
        CoverageResult result = useCase.execute(new Location(27, 912), new Location(2, 12), kg("1"));

        assertEquals(Zone.REST_OF_COUNTRY, ((CoverageResult.Valid) result).zone());
    }

    @Test
    void catalogFailureIsAnExceptionNeverAnInvalidResult() {
        GeoCatalogGateway down = cityId -> {
            throw new CatalogUnavailableException("API Colombia no responde");
        };
        ValidateCoverageUseCase brokenCatalog = new ValidateCoverageUseCase(down, data);

        assertThrows(CatalogUnavailableException.class,
                () -> brokenCatalog.execute(new Location(2, 12), new Location(8, 337), kg("1")));
    }

    @Test
    void missingTariffIsAConfigurationError() {
        CoverageDataGateway withoutTariffs = new CoverageDataGateway() {
            @Override
            public Set<Integer> neighborsOf(int departmentId) {
                return Set.of();
            }

            @Override
            public Optional<Tariff> findTariff(Zone zone) {
                return Optional.empty();
            }
        };
        ValidateCoverageUseCase noTariffs = new ValidateCoverageUseCase(catalog, withoutTariffs);

        assertThrows(TariffNotFoundException.class,
                () -> noTariffs.execute(new Location(2, 12), new Location(8, 337), kg("1")));
    }
}
