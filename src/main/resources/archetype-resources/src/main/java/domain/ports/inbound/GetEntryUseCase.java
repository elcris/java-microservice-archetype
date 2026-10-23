#set( $symbol_dollar = '$' )
package ${package}.domain.ports.inbound;

import java.util.UUID;
import ${package}.domain.models.entry.Entry;

public interface GetEntryUseCase {
  Entry get(UUID id);
}
