#set( $symbol_dollar = '$' )
package ${package}.infrastructure.adapters.inbound.rest.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEntryRequest(@NotBlank @Size(max = 120) String name) {}
