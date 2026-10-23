#set( $symbol_dollar = '$' )
package ${package}.infrastructure.configuration.dynamodb;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.dynamodb")
public record DynamoProperties(
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9_.-]{3,255}") String table, @NotBlank String region) {}
