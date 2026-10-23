#set( $symbol_dollar = '$' )
package ${package}.application.service.entry;

import java.util.UUID;
import org.springframework.stereotype.Service;
import ${package}.domain.exceptions.EntryNotFoundException;
import ${package}.domain.models.entry.Entry;
import ${package}.domain.ports.inbound.GetEntryUseCase;
import ${package}.domain.ports.outbound.LoadEntryPort;

@Service
public class GetEntryService implements GetEntryUseCase {
  private final LoadEntryPort load;

  public GetEntryService(LoadEntryPort load) {
    this.load = load;
  }

  @Override
  public Entry get(UUID id) {
    return load.findById(id).orElseThrow(EntryNotFoundException::new);
  }
}
