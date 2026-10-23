#set( $symbol_dollar = '$' )
package ${package}.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import ${package}.domain.models.entry.Entry;
import ${package}.infrastructure.adapters.outbound.persistence.postgresql.PostgresEntryAdapter;

class PostgresIT {
  @Test
  void migratesAndPreservesIdempotencyAgainstPostgres() {
    // Ephemeral test credentials, generated for this disposable container only.
    String password = UUID.randomUUID().toString();
    try (GenericContainer<?> db =
        new GenericContainer<>("postgres:17-alpine")
            .withEnv("POSTGRES_DB", "testdb")
            .withEnv("POSTGRES_USER", "testuser")
            .withEnv("POSTGRES_PASSWORD", password)
            .withExposedPorts(5432)
            .waitingFor(
                Wait.forLogMessage(".*database system is ready to accept connections.*\n", 2))) {
      db.start();
      String url = "jdbc:postgresql://" + db.getHost() + ":" + db.getMappedPort(5432) + "/testdb";
      Flyway flyway =
          Flyway.configure().dataSource(url, "testuser", password).cleanDisabled(true).load();
      assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
      assertThat(flyway.migrate().migrationsExecuted).isZero();
      flyway.validate();
      var adapter =
          new PostgresEntryAdapter(
              JdbcClient.create(new DriverManagerDataSource(url, "testuser", password)));
      Entry entry = new Entry(UUID.randomUUID(), "sample", Instant.EPOCH);
      assertThat(adapter.findById(entry.id())).isEmpty();
      assertThat(adapter.createIfAbsent(entry).created()).isTrue();
      assertThat(adapter.createIfAbsent(entry).created()).isFalse();
      assertThat(adapter.createIfAbsent(new Entry(entry.id(), "different", Instant.EPOCH)).entry())
          .isEqualTo(entry);
      assertThat(adapter.findById(entry.id())).contains(entry);
    }
  }
}
