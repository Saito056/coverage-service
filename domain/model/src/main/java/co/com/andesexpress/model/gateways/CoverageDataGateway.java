package co.com.andesexpress.model.gateways;
import co.com.andesexpress.model.Tariff;
import co.com.andesexpress.model.Zone;
import java.util.Optional;
import java.util.Set;

public interface CoverageDataGateway {
    Set<Integer> neighborsOf(int departmentId);
    Optional<Tariff> findTariff(Zone zone);
}