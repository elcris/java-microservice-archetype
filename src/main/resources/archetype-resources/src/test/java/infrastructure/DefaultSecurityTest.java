#set( $symbol_dollar = '$' )
package ${package}.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ${package}.domain.ports.inbound.CreateEntryUseCase;
import ${package}.domain.ports.inbound.GetEntryUseCase;
import ${package}.infrastructure.adapters.inbound.rest.v1.controller.EntryController;
import ${package}.infrastructure.adapters.inbound.rest.v1.mapper.EntryRestMapper;
import ${package}.infrastructure.security.SecurityConfiguration;

@WebMvcTest(EntryController.class)
@Import({EntryRestMapper.class, SecurityConfiguration.class})
class DefaultSecurityTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreateEntryUseCase create;
  @MockitoBean GetEntryUseCase getEntry;

  @Test
  void anonymousBusinessApiIsClosedByDefault() throws Exception {
    mvc.perform(get("/api/v1/entries/00000000-0000-0000-0000-000000000001"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("${symbol_dollar}.code").value("ACCESS_DENIED"));
  }

  @Test
  void documentationIsClosedByDefault() throws Exception {
    mvc.perform(get("/openapi.yaml")).andExpect(status().isForbidden());
  }
}
