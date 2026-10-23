#set( $symbol_dollar = '$' )
package ${package}.domain.exceptions;

public final class InvalidEntryException extends RuntimeException {
  public InvalidEntryException() {
    super("Entry is invalid");
  }
}
