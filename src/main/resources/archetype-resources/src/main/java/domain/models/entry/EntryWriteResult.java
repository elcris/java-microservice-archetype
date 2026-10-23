#set( $symbol_dollar = '$' )
package ${package}.domain.models.entry;

public record EntryWriteResult(Entry entry, boolean created) {}
