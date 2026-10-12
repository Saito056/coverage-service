package co.com.andesexpress.seeddata;

import co.com.andesexpress.model.Tariff;
import co.com.andesexpress.model.Zone;
import co.com.andesexpress.model.gateways.CoverageDataGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class CoverageDataAdapter implements CoverageDataGateway {

    private final Map<Integer, Set<Integer>> neighbors;
    private final Map<Zone, Tariff> tariffs = new EnumMap<>(Zone.class);

    public CoverageDataAdapter() {
        try (InputStream in = getClass().getResourceAsStream("/coverage-data.json")) {
            Data data = new ObjectMapper().readValue(in, Data.class);
            this.neighbors = data.neighbors();
            data.tariffs().forEach(t ->
                tariffs.put(t.zone(), new Tariff(t.zone(), t.base(), t.perKg())));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer coverage-data.json", e);
        }
    }

    @Override
    public Set<Integer> neighborsOf(int departmentId) {
        return neighbors.getOrDefault(departmentId, Set.of());
    }

    @Override
    public Optional<Tariff> findTariff(Zone zone) {
        return Optional.ofNullable(tariffs.get(zone));
    }

    record Data(Map<Integer, Set<Integer>> neighbors, List<TariffRow> tariffs) {}
    record TariffRow(Zone zone, BigDecimal base, BigDecimal perKg) {}
}