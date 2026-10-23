#set( $symbol_dollar = '$' )
package ${package}.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment)
      throws Exception {
    boolean demo = environment.acceptsProfiles(Profiles.of("demo"));
    // Demo has no cookies, sessions or browser authentication; see ADR-004.
    if (demo) {
      http.csrf(csrf -> csrf.disable());
    }
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.authorizeHttpRequests(
        auth -> {
          auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
          if (demo) {
            auth.requestMatchers(
                    "/api/v1/entries/**",
                    "/openapi.yaml",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**")
                .permitAll();
          }
          auth.anyRequest().denyAll();
        });
    http.exceptionHandling(
        errors ->
            errors
                .authenticationEntryPoint((req, res, ex) -> forbidden(res))
                .accessDeniedHandler((req, res, ex) -> forbidden(res)));
    return http.build();
  }

  private static void forbidden(HttpServletResponse response) throws IOException {
    response.setStatus(403);
    response.setContentType("application/problem+json");
    response
        .getWriter()
        .write(
            "{\"type\":\"urn:problem:access_denied\",\"title\":\"Forbidden\",\"status\":403,\"code\":\"ACCESS_DENIED\",\"detail\":\"Access"
                + " is not enabled\"}");
  }
}
