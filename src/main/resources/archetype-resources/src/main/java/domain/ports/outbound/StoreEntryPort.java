#set( $symbol_dollar = '$' )
package ${package}.domain.ports.outbound;

import ${package}.domain.models.entry.Entry;
import ${package}.domain.models.entry.EntryWriteResult;

public interface StoreEntryPort {
  /** Atomically inserts when absent; otherwise returns the existing value unchanged. */
  EntryWriteResult createIfAbsent(Entry entry);
}
