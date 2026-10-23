#set( $symbol_dollar = '$' )
package ${package}.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ${package}.application.service.entry.CreateEntryService;
import ${package}.application.service.entry.GetEntryService;
import ${package}.domain.exceptions.EntryConflictException;
import ${package}.domain.exceptions.EntryNotFoundException;
import ${package}.domain.exceptions.StorageUnavailableException;
import ${package}.domain.models.entry.CreateEntryCommand;
import ${package}.domain.models.entry.Entry;
import ${package}.domain.models.entry.EntryWriteResult;
import ${package}.domain.ports.outbound.StoreEntryPort;

class EntryServicesTest {
  private final Map<UUID, Entry> entries = new HashMap<>();
  private final StoreEntryPort store =
      entry -> {
        Entry old = entries.putIfAbsent(entry.id(), entry);
        return new EntryWriteResult(old == null ? entry : old, old == null);
      };
  private final Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void createsAndReplaysWithoutChangingTimestamp() {
    CreateEntryService create = new CreateEntryService(store, clock);
    CreateEntryCommand command = new CreateEntryCommand(UUID.randomUUID(), "sample");
    EntryWriteResult first = create.create(command);
    EntryWriteResult second =
        new CreateEntryService(store, Clock.offset(clock, java.time.Duration.ofHours(1)))
            .create(command);
    assertThat(first.created()).isTrue();
    assertThat(second.created()).isFalse();
    assertThat(second.entry()).isEqualTo(first.entry());
    assertThat(second.entry().createdAt()).isEqualTo(Instant.EPOCH);
    assertThat(entries).hasSize(1);
  }

  @Test
  void rejectsConflictingContent() {
    CreateEntryService create = new CreateEntryService(store, clock);
    UUID id = UUID.randomUUID();
    create.create(new CreateEntryCommand(id, "first"));
    assertThatThrownBy(() -> create.create(new CreateEntryCommand(id, "second")))
        .isInstanceOf(EntryConflictException.class);
    assertThat(entries.get(id).name()).isEqualTo("first");
  }

  @Test
  void loadsAndReportsAbsence() {
    Entry entry = new Entry(UUID.randomUUID(), "sample", Instant.EPOCH);
    entries.put(entry.id(), entry);
    GetEntryService get = new GetEntryService(id -> Optional.ofNullable(entries.get(id)));
    assertThat(get.get(entry.id())).isEqualTo(entry);
    assertThatThrownBy(() -> get.get(UUID.randomUUID())).isInstanceOf(EntryNotFoundException.class);
  }

  @Test
  void propagatesControlledDependencyFailure() {
    CreateEntryService create =
        new CreateEntryService(
            e -> {
              throw new StorageUnavailableException();
            },
            clock);
    assertThatThrownBy(() -> create.create(new CreateEntryCommand(UUID.randomUUID(), "sample")))
        .isInstanceOf(StorageUnavailableException.class);
  }
}
