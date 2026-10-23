#set( $symbol_dollar = '$' )
package ${package}.domain.exceptions;

public final class StorageUnavailableException extends RuntimeException {
  public StorageUnavailableException() {
    super("Storage is temporarily unavailable");
  }
}
