#set( $symbol_dollar = '$' )
package ${package}.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import ${package}.domain.models.entry.Entry;
import ${package}.infrastructure.adapters.outbound.persistence.dynamodb.DynamoEntryAdapter;
import ${package}.infrastructure.configuration.dynamodb.DynamoProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

class DynamoIT {
  @Test
  void conditionalWritesAgainstDynamoLocal() {
    try (GenericContainer<?> db =
        new GenericContainer<>("amazon/dynamodb-local:3.1.0").withExposedPorts(8000)) {
      db.start();
      // Local emulator signature inputs are random and have no AWS account permissions.
      try (DynamoDbClient client =
          DynamoDbClient.builder()
              .endpointOverride(URI.create("http://" + db.getHost() + ":" + db.getMappedPort(8000)))
              .region(Region.US_EAST_1)
              .credentialsProvider(
                  StaticCredentialsProvider.create(
                      AwsBasicCredentials.create(
                          UUID.randomUUID().toString().replace("-", ""),
                          UUID.randomUUID().toString())))
              .build()) {
        String table = "test-entries";
        client.createTable(
            b ->
                b.tableName(table)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(
                        AttributeDefinition.builder()
                            .attributeName("id")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH)
                            .build()));
        client.waiter().waitUntilTableExists(b -> b.tableName(table));
        var adapter = new DynamoEntryAdapter(client, new DynamoProperties(table, "us-east-1"));
        Entry entry = new Entry(UUID.randomUUID(), "sample", Instant.EPOCH);
        assertThat(adapter.findById(entry.id())).isEmpty();
        assertThat(adapter.createIfAbsent(entry).created()).isTrue();
        assertThat(adapter.createIfAbsent(entry).created()).isFalse();
        assertThat(
                adapter.createIfAbsent(new Entry(entry.id(), "different", Instant.EPOCH)).entry())
            .isEqualTo(entry);
        assertThat(adapter.findById(entry.id())).contains(entry);
      }
    }
  }
}
