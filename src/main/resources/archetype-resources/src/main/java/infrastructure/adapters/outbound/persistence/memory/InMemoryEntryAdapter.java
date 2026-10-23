#set( $symbol_dollar = '$' )
package ${package}.infrastructure.adapters.outbound.persistence.memory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ${package}.domain.models.entry.Entry;
import ${package}.domain.models.entry.EntryWriteResult;
import ${package}.domain.ports.outbound.LoadEntryPort;
import ${package}.domain.ports.outbound.StoreEntryPort;

@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "memory")
public class InMemoryEntryAdapter implements StoreEntryPort, LoadEntryPort {
  private final ConcurrentMap<UUID, Entry> entries = new ConcurrentHashMap<>();

  @Override
  public EntryWriteResult createIfAbsent(Entry entry) {
    Entry existing = entries.putIfAbsent(entry.id(), entry);
    return new EntryWriteResult(existing == null ? entry : existing, existing == null);
  }

  @Override
  public Optional<Entry> findById(UUID id) {
    return Optional.ofNullable(entries.get(id));
  }
}
