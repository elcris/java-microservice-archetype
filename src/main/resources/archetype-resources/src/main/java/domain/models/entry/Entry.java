#set( $symbol_dollar = '$' )
package ${package}.domain.models.entry;

import java.time.Instant;
import java.util.UUID;
import ${package}.domain.exceptions.InvalidEntryException;

/** Immutable demonstration resource; no banking semantics or customer data. */
public record Entry(UUID id, String name, Instant createdAt) {
  public Entry {
    if (id == null || name == null || name.isBlank() || name.length() > 120 || createdAt == null) {
      throw new InvalidEntryException();
    }
  }
}
