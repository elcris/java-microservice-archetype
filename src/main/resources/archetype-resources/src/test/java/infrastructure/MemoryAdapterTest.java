#set( $symbol_dollar = '$' )
package ${package}.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import ${package}.domain.models.entry.Entry;
import ${package}.domain.models.entry.EntryWriteResult;
import ${package}.infrastructure.adapters.outbound.persistence.memory.InMemoryEntryAdapter;

class MemoryAdapterTest {
  @Test
  void concurrentWritersCreateExactlyOneValue() throws Exception {
    InMemoryEntryAdapter adapter = new InMemoryEntryAdapter();
    UUID id = UUID.randomUUID();
    assertThat(adapter.findById(id)).isEmpty();
    try (var pool = Executors.newFixedThreadPool(8)) {
      List<Callable<EntryWriteResult>> calls =
          IntStream.range(0, 32)
              .mapToObj(
                  i ->
                      (Callable<EntryWriteResult>)
                          () -> adapter.createIfAbsent(new Entry(id, "sample", Instant.EPOCH)))
              .toList();
      List<Future<EntryWriteResult>> results = pool.invokeAll(calls);
      int created = 0;
      for (Future<EntryWriteResult> result : results) {
        if (result.get().created()) {
          created++;
        }
      }
      assertThat(created).isEqualTo(1);
      assertThat(adapter.findById(id)).contains(new Entry(id, "sample", Instant.EPOCH));
    }
  }
}
