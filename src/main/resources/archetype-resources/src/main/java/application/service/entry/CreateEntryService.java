#set( $symbol_dollar = '$' )
package ${package}.application.service.entry;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import ${package}.domain.exceptions.EntryConflictException;
import ${package}.domain.models.entry.CreateEntryCommand;
import ${package}.domain.models.entry.Entry;
import ${package}.domain.models.entry.EntryWriteResult;
import ${package}.domain.ports.inbound.CreateEntryUseCase;
import ${package}.domain.ports.outbound.StoreEntryPort;

@Service
public class CreateEntryService implements CreateEntryUseCase {
  private final StoreEntryPort store;
  private final Clock clock;

  public CreateEntryService(StoreEntryPort store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  @Override
  public EntryWriteResult create(CreateEntryCommand command) {
    EntryWriteResult result =
        store.createIfAbsent(new Entry(command.id(), command.name(), Instant.now(clock)));
    if (!result.entry().name().equals(command.name())) {
      throw new EntryConflictException();
    }
    return result;
  }
}
