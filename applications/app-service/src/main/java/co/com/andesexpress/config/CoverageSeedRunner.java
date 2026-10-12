package co.com.andesexpress.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import co.com.andesexpress.dynamodb.DynamoCoverageDataSeeder;
import co.com.andesexpress.seeddata.CoverageDataAdapter;

/**
 * Con coverage.data.source=dynamodb y coverage.dynamodb.seed-on-startup=true, carga en la tabla
 * lo que falte a partir de coverage-data.json. No pisa datos existentes.
 */
@Component
@ConditionalOnProperty(name = "coverage.data.source", havingValue = "dynamodb")
public class CoverageSeedRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageSeedRunner.class);

    private final DynamoCoverageDataSeeder seeder;
    private final boolean enabled;

    public CoverageSeedRunner(DynamoCoverageDataSeeder seeder,
                              @Value("${coverage.dynamodb.seed-on-startup:false}") boolean enabled) {
        this.seeder = seeder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        int written = seeder.seedMissing(new CoverageDataAdapter());
        LOG.info("Carga inicial de coverage-data.json en DynamoDB: {} items escritos", written);
    }
}
