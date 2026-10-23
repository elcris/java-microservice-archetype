#set( $symbol_dollar = '$' )
package ${package}.infrastructure.adapters.inbound.rest.v1.dto;

import java.time.Instant;
import java.util.UUID;

public record EntryResponse(UUID id, String name, Instant createdAt) {}
