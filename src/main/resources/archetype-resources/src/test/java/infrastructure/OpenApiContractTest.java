#set( $symbol_dollar = '$' )
package ${package}.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {
  @Test
  void specificationIsValidAndDocumentsImplementedResponses() {
    ParseOptions options = new ParseOptions();
    options.setResolve(true);
    var parsed =
        new OpenAPIV3Parser()
            .readLocation("src/main/resources/openapi/openapi.yaml", null, options);
    assertThat(parsed.getMessages()).isEmpty();
    var api = parsed.getOpenAPI();
    assertThat(api).isNotNull();
    assertThat(api.getOpenapi()).isEqualTo("3.1.1");
    assertThat(api.getPaths()).containsOnlyKeys("/api/v1/entries/{id}");
    var path = api.getPaths().get("/api/v1/entries/{id}");
    assertThat(path.getPut().getResponses())
        .containsKeys("200", "201", "400", "403", "409", "415", "422", "500", "503");
    assertThat(path.getGet().getResponses()).containsKeys("200", "400", "403", "404", "500", "503");
    assertThat(api.getComponents().getSchemas().get("Entry").getRequired().toArray())
        .containsExactlyInAnyOrder("id", "name", "createdAt");
  }
}
