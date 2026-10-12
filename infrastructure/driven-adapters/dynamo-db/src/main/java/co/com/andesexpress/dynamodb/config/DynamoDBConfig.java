package co.com.andesexpress.dynamodb.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import co.com.andesexpress.dynamodb.CoverageConfigStore;
import co.com.andesexpress.dynamodb.DynamoCoverageConfigStore;
import co.com.andesexpress.dynamodb.DynamoCoverageDataAdapter;
import co.com.andesexpress.dynamodb.DynamoCoverageDataSeeder;
import co.com.andesexpress.model.gateways.CoverageDataGateway;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

/**
 * Solo se activa con coverage.data.source=dynamodb. Con "json" (valor por defecto) nada de esto
 * se crea y el servicio arranca sin AWS.
 *
 * Credenciales: cadena por defecto del SDK (variables de entorno, perfil ~/.aws, rol de IAM del
 * contenedor/instancia). Si aws.dynamodb.endpoint esta definido se asume DynamoDB Local y se usan
 * credenciales de mentira; ese endpoint solo debe usarse en desarrollo.
 */
@Configuration
@ConditionalOnProperty(name = "coverage.data.source", havingValue = "dynamodb")
public class DynamoDBConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(@Value("${aws.region}") String region,
                                         @Value("${aws.dynamodb.endpoint:}") String endpoint,
                                         @Value("${coverage.dynamodb.call-timeout-ms:1500}") long callTimeoutMs,
                                         @Value("${coverage.dynamodb.attempt-timeout-ms:700}") long attemptTimeoutMs) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder();
        builder.region(Region.of(region));
        builder.overrideConfiguration(ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMillis(callTimeoutMs))
                .apiCallAttemptTimeout(Duration.ofMillis(attemptTimeoutMs))
                .build());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Bean
    public CoverageConfigStore coverageConfigStore(DynamoDbEnhancedClient client,
                                                   @Value("${coverage.dynamodb.table:coverage-config}") String table) {
        return new DynamoCoverageConfigStore(client, table);
    }

    @Bean
    public DynamoCoverageDataSeeder dynamoCoverageDataSeeder(CoverageConfigStore store) {
        return new DynamoCoverageDataSeeder(store);
    }

    /** @Primary: gana sobre el adaptador del JSON, que sigue existiendo como bean. */
    @Bean
    @Primary
    public CoverageDataGateway dynamoCoverageData(CoverageConfigStore store,
                                                  @Value("${coverage.dynamodb.cache-ttl-seconds:300}") long cacheTtlSeconds) {
        return new DynamoCoverageDataAdapter(store, Duration.ofSeconds(cacheTtlSeconds));
    }
}