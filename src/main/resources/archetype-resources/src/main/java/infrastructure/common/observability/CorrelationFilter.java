#set( $symbol_dollar = '$' )
package ${package}.infrastructure.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter extends OncePerRequestFilter {
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String supplied = req.getHeader("X-Correlation-ID");
    String id =
        supplied != null && supplied.matches("[A-Za-z0-9-]{1,64}")
            ? supplied
            : UUID.randomUUID().toString();
    String previous = MDC.get("correlationId");
    long start = System.nanoTime();
    MDC.put("correlationId", id);
    res.setHeader("X-Correlation-ID", id);
    try {
      chain.doFilter(req, res);
    } finally {
      LOG.atInfo()
          .addKeyValue("method", req.getMethod())
          .addKeyValue("status", res.getStatus())
          .addKeyValue("durationMs", (System.nanoTime() - start) / 1_000_000)
          .log("http.request.completed");
      if (previous == null) {
        MDC.remove("correlationId");
      } else {
        MDC.put("correlationId", previous);
      }
    }
  }
}
