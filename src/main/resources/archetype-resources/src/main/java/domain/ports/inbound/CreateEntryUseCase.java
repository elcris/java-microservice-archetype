#set( $symbol_dollar = '$' )
package ${package}.domain.ports.inbound;

import ${package}.domain.models.entry.CreateEntryCommand;
import ${package}.domain.models.entry.EntryWriteResult;

public interface CreateEntryUseCase {
  EntryWriteResult create(CreateEntryCommand command);
}
