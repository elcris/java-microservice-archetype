#set( $symbol_dollar = '$' )
package ${package}.infrastructure.configuration.dynamodb;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@ConditionalOnProperty(name = "app.storage", havingValue = "dynamodb")
@EnableConfigurationProperties(DynamoProperties.class)
public class DynamoConfiguration {
  @Bean(destroyMethod = "close")
  DynamoDbClient dynamoDbClient(DynamoProperties properties) {
    return DynamoDbClient.builder()
        .region(Region.of(properties.region()))
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .httpClientBuilder(
            UrlConnectionHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(2))
                .socketTimeout(Duration.ofSeconds(3)))
        .overrideConfiguration(
            c ->
                c.apiCallTimeout(Duration.ofSeconds(5))
                    .apiCallAttemptTimeout(Duration.ofSeconds(3)))
        .build();
  }
}
