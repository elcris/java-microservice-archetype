#set( $symbol_dollar = '$' )
package ${package}.domain.exceptions;

public final class EntryNotFoundException extends RuntimeException {
  public EntryNotFoundException() {
    super("Entry was not found");
  }
}
