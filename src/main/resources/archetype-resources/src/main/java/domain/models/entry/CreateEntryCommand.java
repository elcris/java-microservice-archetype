#set( $symbol_dollar = '$' )
package ${package}.domain.models.entry;

import java.util.UUID;

public record CreateEntryCommand(UUID id, String name) {}
