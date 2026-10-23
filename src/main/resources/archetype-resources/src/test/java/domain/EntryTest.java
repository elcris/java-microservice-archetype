#set( $symbol_dollar = '$' )
package ${package}.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ${package}.domain.exceptions.InvalidEntryException;
import ${package}.domain.models.entry.Entry;

class EntryTest {
  @Test
  void rejectsInvalidState() {
    UUID id = UUID.randomUUID();
    assertThatThrownBy(() -> new Entry(id, " ", Instant.EPOCH))
        .isInstanceOf(InvalidEntryException.class);
    assertThatThrownBy(() -> new Entry(id, "x".repeat(121), Instant.EPOCH))
        .isInstanceOf(InvalidEntryException.class);
    assertThatThrownBy(() -> new Entry(null, "sample", Instant.EPOCH))
        .isInstanceOf(InvalidEntryException.class);
    assertThatThrownBy(() -> new Entry(id, null, Instant.EPOCH))
        .isInstanceOf(InvalidEntryException.class);
    assertThatThrownBy(() -> new Entry(id, "sample", null))
        .isInstanceOf(InvalidEntryException.class);
  }
}
