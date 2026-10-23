#set( $symbol_dollar = '$' )
package ${package}.infrastructure.adapters.inbound.rest.v1.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ${package}.domain.models.entry.CreateEntryCommand;
import ${package}.domain.models.entry.EntryWriteResult;
import ${package}.domain.ports.inbound.CreateEntryUseCase;
import ${package}.domain.ports.inbound.GetEntryUseCase;
import ${package}.infrastructure.adapters.inbound.rest.v1.dto.CreateEntryRequest;
import ${package}.infrastructure.adapters.inbound.rest.v1.dto.EntryResponse;
import ${package}.infrastructure.adapters.inbound.rest.v1.mapper.EntryRestMapper;

@RestController
@RequestMapping("/api/v1/entries")
public class EntryController {
  private final CreateEntryUseCase create;
  private final GetEntryUseCase get;
  private final EntryRestMapper mapper;

  public EntryController(CreateEntryUseCase create, GetEntryUseCase get, EntryRestMapper mapper) {
    this.create = create;
    this.get = get;
    this.mapper = mapper;
  }

  @PutMapping("/{id}")
  public ResponseEntity<EntryResponse> create(
      @PathVariable UUID id, @Valid @RequestBody CreateEntryRequest request) {
    EntryWriteResult result = create.create(new CreateEntryCommand(id, request.name()));
    EntryResponse body = mapper.toResponse(result.entry());
    return result.created()
        ? ResponseEntity.created(URI.create("/api/v1/entries/" + body.id())).body(body)
        : ResponseEntity.ok(body);
  }

  @GetMapping("/{id}")
  public EntryResponse get(@PathVariable UUID id) {
    return mapper.toResponse(get.get(id));
  }
}
