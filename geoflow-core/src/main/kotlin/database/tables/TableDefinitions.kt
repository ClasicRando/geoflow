package database.tables

import java.io.InputStream

/** Base interface for interfaces used to create tables in the buildScript file. Helps reflection operations */
interface TableBuildRequirement

/**
 * Table should be created with default data during build script operation
 */
interface DefaultData : TableBuildRequirement {
    /** Name of the file in the resources folder that contains the default data needed to be loaded */
    val defaultRecordsFileName: String
}

/** Extends default data tables to have easy access to the resources file specified */
val DefaultData.defaultRecordsFile: InputStream?
    get() = this::class.java.classLoader.getResourceAsStream(defaultRecordsFileName)

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
 * Data Class to hold the trigger create statement and trigger function create statement
 */
data class Trigger(val trigger: String, val triggerFunction: String)

/**
 * Table contains triggers. Defined as a list of Trigger data classes
 */
interface Triggers : TableBuildRequirement {
    /**
     * List of triggers on a Table.
     */
    val triggers: List<Trigger>
}
