#set( $symbol_dollar = '$' )
package ${package}.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ${package}.domain.exceptions.EntryConflictException;
import ${package}.domain.exceptions.EntryNotFoundException;
import ${package}.domain.exceptions.StorageUnavailableException;
import ${package}.domain.models.entry.Entry;
import ${package}.domain.models.entry.EntryWriteResult;
import ${package}.domain.ports.inbound.CreateEntryUseCase;
import ${package}.domain.ports.inbound.GetEntryUseCase;
import ${package}.infrastructure.adapters.inbound.rest.v1.controller.EntryController;
import ${package}.infrastructure.adapters.inbound.rest.v1.mapper.EntryRestMapper;
import ${package}.infrastructure.common.exception.ApiExceptionHandler;
import ${package}.infrastructure.common.observability.CorrelationFilter;
import ${package}.infrastructure.security.SecurityConfiguration;

@WebMvcTest(EntryController.class)
@ActiveProfiles("demo")
@Import({
  EntryRestMapper.class,
  ApiExceptionHandler.class,
  CorrelationFilter.class,
  SecurityConfiguration.class
})
class EntryControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreateEntryUseCase create;
  @MockitoBean GetEntryUseCase getEntry;
  private final UUID id = UUID.randomUUID();

  @Test
  void createsWithLocationAndDirectResource() throws Exception {
    when(create.create(any()))
        .thenReturn(new EntryWriteResult(new Entry(id, "sample", Instant.EPOCH), true));
    mvc.perform(
            put("/api/v1/entries/" + id)
                .contentType("application/json")
                .header("X-Correlation-ID", "test-correlation")
                .content("{\"name\":\"sample\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/entries/" + id))
        .andExpect(header().string("X-Correlation-ID", "test-correlation"))
        .andExpect(jsonPath("${symbol_dollar}.id").value(id.toString()))
        .andExpect(jsonPath("${symbol_dollar}.data").doesNotExist());
  }

  @Test
  void validatesBlankName() throws Exception {
    mvc.perform(
            put("/api/v1/entries/" + id)
                .contentType("application/json")
                .content("{\"name\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("${symbol_dollar}.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("${symbol_dollar}.errors[0].field").value("name"));
  }

  @Test
  void malformedJsonAndUuidAre400() throws Exception {
    mvc.perform(put("/api/v1/entries/" + id).contentType("application/json").content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("${symbol_dollar}.code").value("HTTP_400"));
    mvc.perform(get("/api/v1/entries/not-a-uuid")).andExpect(status().isBadRequest());
  }

  @Test
  void notFoundIs404() throws Exception {
    when(getEntry.get(any())).thenThrow(new EntryNotFoundException());
    mvc.perform(get("/api/v1/entries/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("${symbol_dollar}.code").value("ENTRY_NOT_FOUND"));
  }

  @Test
  void replayIs200() throws Exception {
    when(create.create(any()))
        .thenReturn(new EntryWriteResult(new Entry(id, "sample", Instant.EPOCH), false));
    mvc.perform(
            put("/api/v1/entries/" + id)
                .contentType("application/json")
                .content("{\"name\":\"sample\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void conflictIs409() throws Exception {
    when(create.create(any())).thenThrow(new EntryConflictException());
    mvc.perform(
            put("/api/v1/entries/" + id)
                .contentType("application/json")
                .content("{\"name\":\"sample\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("${symbol_dollar}.code").value("ENTRY_CONFLICT"));
  }

  @Test
  void dependencyFailureIs503() throws Exception {
    when(getEntry.get(any())).thenThrow(new StorageUnavailableException());
    mvc.perform(get("/api/v1/entries/" + id)).andExpect(status().isServiceUnavailable());
  }

  @Test
  void unexpectedFailureDoesNotLeakDetails() throws Exception {
    when(getEntry.get(any())).thenThrow(new IllegalStateException("private-detail"));
    mvc.perform(get("/api/v1/entries/" + id))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("${symbol_dollar}.detail").value("Unexpected error"));
  }

  @Test
  void rejectsWrongMethodAndMediaType() throws Exception {
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/v1/entries/" + id))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("${symbol_dollar}.code").value("HTTP_405"));
    mvc.perform(put("/api/v1/entries/" + id).contentType("text/plain").content("sample"))
        .andExpect(status().isUnsupportedMediaType());
  }
}
