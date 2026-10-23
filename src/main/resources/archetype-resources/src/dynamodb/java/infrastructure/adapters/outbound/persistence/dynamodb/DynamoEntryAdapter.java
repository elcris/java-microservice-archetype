#set( $symbol_dollar = '$' )
package ${package}.infrastructure.adapters.outbound.persistence.dynamodb;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ${package}.domain.exceptions.StorageUnavailableException;
import ${package}.domain.models.entry.Entry;
import ${package}.domain.models.entry.EntryWriteResult;
import ${package}.domain.ports.outbound.LoadEntryPort;
import ${package}.domain.ports.outbound.StoreEntryPort;
import ${package}.infrastructure.configuration.dynamodb.DynamoProperties;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "dynamodb")
public class DynamoEntryAdapter implements StoreEntryPort, LoadEntryPort {
  private final DynamoDbClient client;
  private final String table;

  public DynamoEntryAdapter(DynamoDbClient client, DynamoProperties properties) {
    this.client = client;
    this.table = properties.table();
  }

  @Override
  public EntryWriteResult createIfAbsent(Entry entry) {
    try {
      client.putItem(
          PutItemRequest.builder()
              .tableName(table)
              .item(
                  Map.of(
                      "id",
                      value(entry.id().toString()),
                      "name",
                      value(entry.name()),
                      "createdAt",
                      value(entry.createdAt().toString())))
              .conditionExpression("attribute_not_exists(id)")
              .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
              .build());
      return new EntryWriteResult(entry, true);
    } catch (ConditionalCheckFailedException ex) {
      Entry existing =
          ex.hasItem()
              ? toEntry(ex.item())
              : findById(entry.id()).orElseThrow(StorageUnavailableException::new);
      return new EntryWriteResult(existing, false);
    } catch (SdkException ex) {
      throw new StorageUnavailableException();
    }
  }

  @Override
  public Optional<Entry> findById(UUID id) {
    try {
      Map<String, AttributeValue> item =
          client
              .getItem(
                  GetItemRequest.builder()
                      .tableName(table)
                      .key(Map.of("id", value(id.toString())))
                      .consistentRead(true)
                      .build())
              .item();
      return item.isEmpty() ? Optional.empty() : Optional.of(toEntry(item));
    } catch (SdkException ex) {
      throw new StorageUnavailableException();
    }
  }

  private static AttributeValue value(String text) {
    return AttributeValue.builder().s(text).build();
  }

  private static Entry toEntry(Map<String, AttributeValue> item) {
    try {
      return new Entry(
          UUID.fromString(item.get("id").s()),
          item.get("name").s(),
          Instant.parse(item.get("createdAt").s()));
    } catch (RuntimeException ex) {
      throw new StorageUnavailableException();
    }
  }
}
