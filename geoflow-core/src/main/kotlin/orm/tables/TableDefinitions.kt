package orm.tables

/**
 * Table is used to return results in the API
 */
interface ApiExposed {
    /**
     * Fields provided when this table is used in the server API to display in a bootstrap table.
     *
     * Each key to the outer map is the field name (or JSON key from the API response), and the inner map is properties
     * of the field (as described here [column-options](https://bootstrap-table.com/docs/api/column-options/)) with the
     * 'data-' prefix automatically added during table HTML creation
     */
    val tableDisplayFields: Map<String, Map<String, String>>
}

/**
 * Table's primary key is a sequential value. Tells build script to look through create statement to make sequence
 */
interface SequentialPrimaryKey

/**
 * Data Class to hold the trigger create statement and trigger function create statement
 */
data class Trigger(val trigger: String, val triggerFunction: String)

/**
 * Table contains triggers. Defined as a list of Trigger data classes
 */
interface Triggers {
    /**
     * List of triggers on a Table.
     */
    val triggers: List<Trigger>
}
