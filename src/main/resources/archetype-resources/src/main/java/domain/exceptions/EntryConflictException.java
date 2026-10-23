#set( $symbol_dollar = '$' )
package ${package}.domain.exceptions;

public final class EntryConflictException extends RuntimeException {
  public EntryConflictException() {
    super("Identifier already contains a different entry");
  }
}
