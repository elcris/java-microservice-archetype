#set( $symbol_dollar = '$' )
package ${package}.infrastructure.common.exception;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ${package}.domain.exceptions.EntryConflictException;
import ${package}.domain.exceptions.EntryNotFoundException;
import ${package}.domain.exceptions.InvalidEntryException;
import ${package}.domain.exceptions.StorageUnavailableException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private ProblemDetail problem(HttpStatusCode status, String code, String detail, WebRequest req) {
    ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
    p.setType(URI.create("urn:problem:" + code.toLowerCase(java.util.Locale.ROOT)));
    p.setProperty("code", code);
    p.setProperty("correlationId", MDC.get("correlationId"));
    p.setInstance(URI.create(((ServletWebRequest) req).getRequest().getRequestURI()));
    return p;
  }

  @ExceptionHandler(EntryNotFoundException.class)
  ProblemDetail notFound(EntryNotFoundException ex, WebRequest req) {
    return problem(HttpStatus.NOT_FOUND, "ENTRY_NOT_FOUND", ex.getMessage(), req);
  }

  @ExceptionHandler(EntryConflictException.class)
  ProblemDetail conflict(EntryConflictException ex, WebRequest req) {
    return problem(HttpStatus.CONFLICT, "ENTRY_CONFLICT", ex.getMessage(), req);
  }

  @ExceptionHandler(InvalidEntryException.class)
  ProblemDetail invalid(InvalidEntryException ex, WebRequest req) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, "ENTRY_INVALID", ex.getMessage(), req);
  }

  @ExceptionHandler(StorageUnavailableException.class)
  ProblemDetail unavailable(StorageUnavailableException ex, WebRequest req) {
    return problem(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE", ex.getMessage(), req);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception ex, WebRequest req) {
    // Exception messages and stack traces can contain SQL, payloads or secrets.
    LOG.atError().addKeyValue("exceptionType", ex.getClass().getSimpleName()).log("request.failed");
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", req);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest req) {
    ProblemDetail p = problem(status, "VALIDATION_ERROR", "Request validation failed", req);
    List<Map<String, String>> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                e ->
                    Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
            .sorted(java.util.Comparator.comparing(e -> e.get("field")))
            .toList();
    p.setProperty("errors", errors);
    return new ResponseEntity<>(p, headers, status);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest req) {
    return new ResponseEntity<>(
        problem(status, "HTTP_" + status.value(), "Request could not be processed", req),
        headers,
        status);
  }
}
