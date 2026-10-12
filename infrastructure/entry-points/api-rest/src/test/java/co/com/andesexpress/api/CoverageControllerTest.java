package co.com.andesexpress.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.com.andesexpress.model.CatalogUnavailableException;
import co.com.andesexpress.model.Tariff;
import co.com.andesexpress.model.Zone;
import co.com.andesexpress.model.gateways.CoverageDataGateway;
import co.com.andesexpress.model.gateways.GeoCatalogGateway;
import co.com.andesexpress.usecase.validatecoverage.ValidateCoverageUseCase;

/**
 * Prueba el contrato Orders <-> Coverage extremo a extremo (ruta, JSON y codigos HTTP),
 * con el caso de uso real y puertos falsos (sin red ni Spring completo).
 */
class CoverageControllerTest {

    private static final String ORDERS_REQUEST = """
            {"origin":{"departmentId":2,"cityId":12},
             "destination":{"departmentId":8,"cityId":337},
             "totalWeightKg":10.5}""";

    private static final Map<Integer, Integer> CITY_TO_DEPARTMENT = Map.of(12, 2, 337, 8);

    private static final CoverageDataGateway DATA = new CoverageDataGateway() {
        @Override
        public Set<Integer> neighborsOf(int departmentId) {
            return departmentId == 2 ? Set.of(6, 7, 8, 13, 14, 26, 28) : Set.of();
        }

        @Override
        public Optional<Tariff> findTariff(Zone zone) {
            return Optional.of(new Tariff(zone,
                    new java.math.BigDecimal("14000"), new java.math.BigDecimal("1200")));
        }
    };

    private MockMvc mockMvc(GeoCatalogGateway catalog) {
        return MockMvcBuilders
                .standaloneSetup(new CoverageController(new ValidateCoverageUseCase(catalog, DATA)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private MockMvc mockMvc() {
        return mockMvc(cityId -> Optional.ofNullable(CITY_TO_DEPARTMENT.get(cityId)));
    }

    private MockHttpServletResponse send(MockMvc mvc, String body) throws Exception {
        return mvc.perform(post("/coverage/validate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse();
    }

    @Test
    void validRequestReturns200WithTheContractShape() throws Exception {
        MockHttpServletResponse response = send(mockMvc(), ORDERS_REQUEST);

        String body = response.getContentAsString();
        assertEquals(200, response.getStatus());
        assertTrue(body.contains("\"valid\":true"), body);
        assertTrue(body.contains("\"zone\":\"NEIGHBORING_DEPARTMENT\""), body);
        assertTrue(body.contains("\"fare\":{\"amount\":26600,\"currency\":\"COP\"}"), body);
        assertFalse(body.contains("reason"), body);
    }

    @Test
    void unknownDestinationIsABusinessResultWith200() throws Exception {
        String request = ORDERS_REQUEST.replace("337", "99999999");

        MockHttpServletResponse response = send(mockMvc(), request);

        String body = response.getContentAsString();
        assertEquals(200, response.getStatus());
        assertTrue(body.contains("\"valid\":false"), body);
        assertTrue(body.contains("\"reason\":\"DESTINATION_CITY_NOT_FOUND\""), body);
        assertTrue(body.contains("\"message\":"), body);
        assertFalse(body.contains("fare"), body);
    }

    @Test
    void catalogDownReturns503() throws Exception {
        GeoCatalogGateway down = cityId -> {
            throw new CatalogUnavailableException("API Colombia no responde");
        };

        MockHttpServletResponse response = send(mockMvc(down), ORDERS_REQUEST);

        assertEquals(503, response.getStatus());
    }

    @Test
    void oldFlatFormatIsRejectedWith400() throws Exception {
        String flat = "{\"originDepartmentId\":2,\"originCityId\":12,"
                + "\"destinationDepartmentId\":8,\"destinationCityId\":337,\"weightKg\":10.5}";

        assertEquals(400, send(mockMvc(), flat).getStatus());
    }

    @Test
    void zeroWeightReturns400() throws Exception {
        String request = ORDERS_REQUEST.replace("10.5", "0");

        assertEquals(400, send(mockMvc(), request).getStatus());
    }

    @Test
    void missingBodyFieldsReturn400() throws Exception {
        assertEquals(400, send(mockMvc(), "{}").getStatus());
    }
}
