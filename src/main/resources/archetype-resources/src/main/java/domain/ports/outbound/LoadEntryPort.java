#set( $symbol_dollar = '$' )
package ${package}.domain.ports.outbound;

import java.util.Optional;
import java.util.UUID;
import ${package}.domain.models.entry.Entry;

public interface LoadEntryPort {
  /** Empty means absent; failures throw StorageUnavailableException, never return null. */
  Optional<Entry> findById(UUID id);
}
