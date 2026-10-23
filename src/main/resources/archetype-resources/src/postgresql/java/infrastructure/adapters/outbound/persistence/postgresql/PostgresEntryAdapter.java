#set( $symbol_dollar = '$' )
package ${package}.infrastructure.adapters.outbound.persistence.postgresql;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ${package}.domain.exceptions.StorageUnavailableException;
import ${package}.domain.models.entry.Entry;
import ${package}.domain.models.entry.EntryWriteResult;
import ${package}.domain.ports.outbound.LoadEntryPort;
import ${package}.domain.ports.outbound.StoreEntryPort;

@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "postgresql")
public class PostgresEntryAdapter implements StoreEntryPort, LoadEntryPort {
  private final JdbcClient jdbc;

  public PostgresEntryAdapter(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public EntryWriteResult createIfAbsent(Entry entry) {
    try {
      int changed =
          jdbc.sql(
                  """
                  INSERT INTO demo_entries (id, name, created_at) VALUES (:id, :name, :created)
                  ON CONFLICT (id) DO NOTHING
                  """)
              .param("id", entry.id())
              .param("name", entry.name())
              .param("created", Timestamp.from(entry.createdAt()))
              .update();
      // Read the stored timestamp as PostgreSQL precision may differ from the JVM clock.
      return new EntryWriteResult(
          findById(entry.id()).orElseThrow(StorageUnavailableException::new), changed == 1);
    } catch (DataAccessException ex) {
      throw new StorageUnavailableException();
    }
  }

  @Override
  public Optional<Entry> findById(UUID id) {
    try {
      return jdbc.sql("SELECT id, name, created_at FROM demo_entries WHERE id = :id")
          .param("id", id)
          .query(
              (rs, row) ->
                  new Entry(
                      rs.getObject("id", UUID.class),
                      rs.getString("name"),
                      rs.getTimestamp("created_at").toInstant()))
          .optional();
    } catch (DataAccessException ex) {
      throw new StorageUnavailableException();
    }
  }
}
