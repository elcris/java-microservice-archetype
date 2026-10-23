#set( $symbol_dollar = '$' )
package ${package}.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import ${package}.domain.exceptions.StorageUnavailableException;
import ${package}.domain.models.entry.Entry;
import ${package}.infrastructure.adapters.outbound.persistence.postgresql.PostgresEntryAdapter;

class PostgresFailureTest {
  @Test
  void databaseExceptionsDoNotCrossPort() {
    JdbcClient jdbc = mock(JdbcClient.class);
    when(jdbc.sql(anyString()))
        .thenThrow(new DataAccessResourceFailureException("private SQL details"));
    PostgresEntryAdapter adapter = new PostgresEntryAdapter(jdbc);
    assertThatThrownBy(() -> adapter.findById(UUID.randomUUID()))
        .isInstanceOf(StorageUnavailableException.class);
    assertThatThrownBy(
            () -> adapter.createIfAbsent(new Entry(UUID.randomUUID(), "sample", Instant.EPOCH)))
        .isInstanceOf(StorageUnavailableException.class);
  }
}
