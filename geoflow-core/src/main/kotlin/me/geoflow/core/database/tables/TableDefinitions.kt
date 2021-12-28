package me.geoflow.core.database.tables

import java.io.InputStream

/** Base interface for interfaces used to create tables in the buildScript file. Helps reflection operations */
interface TableBuildRequirement

/** Table should be created with default data during build script operation */
interface DefaultData : TableBuildRequirement {
    /** Name of the file in the resources folder that contains the default data needed to be loaded */
    val defaultRecordsFileName: String
}

/** Extends default data tables to have easy access to the resources file specified */
val DefaultData.defaultRecordsFile: InputStream?
    get() = this::class.java.classLoader.getResourceAsStream(defaultRecordsFileName)

/** Marks an object as contains field information for a bootstrapTable. Usually applied to a [DbTable] */
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

/** Data Class to hold the trigger create statement and trigger function create statement */
data class Trigger(
    /** create statement of trigger */
    val trigger: String,
    /** function create statement that is called by the trigger */
    val triggerFunction: String,
)

/** Table contains triggers. Defined as a list of Trigger data classes */
interface Triggers : TableBuildRequirement {
    /**
     * List of triggers on a Table.
     */
    val triggers: List<Trigger>
}

/** Table should be created with default data generated during build script operation */
interface DefaultGeneratedData {
    /** Name of the file in the resources folder that contains the data generation strategy */
    val dataGenerationSqlFile: String
}

/** Extends default data tables to have easy access to the resources file specified for data generation */
val DefaultGeneratedData.dataGenerationSql: InputStream?
    get() = this::class.java.classLoader.getResourceAsStream(dataGenerationSqlFile)
