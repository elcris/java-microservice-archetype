#set( $symbol_dollar = '$' )
package ${package}.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ${package}.domain.exceptions.StorageUnavailableException;
import ${package}.domain.models.entry.Entry;
import ${package}.infrastructure.adapters.outbound.persistence.dynamodb.DynamoEntryAdapter;
import ${package}.infrastructure.configuration.dynamodb.DynamoProperties;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

class DynamoAdapterTest {
  private final DynamoDbClient client = mock(DynamoDbClient.class);
  private final DynamoEntryAdapter adapter =
      new DynamoEntryAdapter(client, new DynamoProperties("test-table", "us-east-1"));
  private final Entry entry = new Entry(UUID.randomUUID(), "sample", Instant.EPOCH);

  private Map<String, AttributeValue> item() {
    return Map.of(
        "id",
        AttributeValue.fromS(entry.id().toString()),
        "name",
        AttributeValue.fromS(entry.name()),
        "createdAt",
        AttributeValue.fromS(entry.createdAt().toString()));
  }

  @Test
  void writesConditionally() {
    assertThat(adapter.createIfAbsent(entry).created()).isTrue();
    ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
    verify(client).putItem(captor.capture());
    assertThat(captor.getValue().conditionExpression()).isEqualTo("attribute_not_exists(id)");
    assertThat(captor.getValue().item()).isEqualTo(item());
  }

  @Test
  void returnsExistingOnConcurrentDuplicate() {
    when(client.putItem(any(PutItemRequest.class)))
        .thenThrow(ConditionalCheckFailedException.builder().item(item()).build());
    var result = adapter.createIfAbsent(entry);
    assertThat(result.created()).isFalse();
    assertThat(result.entry()).isEqualTo(entry);
  }

  @Test
  void readsConsistentlyAndHandlesAbsence() {
    when(client.getItem(any(GetItemRequest.class)))
        .thenReturn(GetItemResponse.builder().item(item()).build());
    assertThat(adapter.findById(entry.id())).contains(entry);
    ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
    verify(client).getItem(captor.capture());
    assertThat(captor.getValue().consistentRead()).isTrue();
    when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
    assertThat(adapter.findById(entry.id())).isEmpty();
  }

  @Test
  void translatesAwsFailure() {
    when(client.getItem(any(GetItemRequest.class)))
        .thenThrow(SdkClientException.create("private details"));
    assertThatThrownBy(() -> adapter.findById(entry.id()))
        .isInstanceOf(StorageUnavailableException.class);
    when(client.putItem(any(PutItemRequest.class)))
        .thenThrow(SdkClientException.create("private details"));
    assertThatThrownBy(() -> adapter.createIfAbsent(entry))
        .isInstanceOf(StorageUnavailableException.class);
  }
}
