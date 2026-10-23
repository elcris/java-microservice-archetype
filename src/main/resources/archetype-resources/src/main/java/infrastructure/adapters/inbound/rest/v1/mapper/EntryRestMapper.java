#set( $symbol_dollar = '$' )
package ${package}.infrastructure.adapters.inbound.rest.v1.mapper;

import org.springframework.stereotype.Component;
import ${package}.domain.models.entry.Entry;
import ${package}.infrastructure.adapters.inbound.rest.v1.dto.EntryResponse;

@Component
public class EntryRestMapper {
  public EntryResponse toResponse(Entry entry) {
    return new EntryResponse(entry.id(), entry.name(), entry.createdAt());
  }
}
