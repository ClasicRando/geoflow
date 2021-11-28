package database.tables

/**
 * Base definition of a database table. Contains some constraints and default implementations of properties used by
 * the BuildScript.
 *
 * @param tableName name of the table in the database
 */
abstract class DbTable(val tableName: String) {
    /**
     * SQL create statement for the table
     */
    abstract val createStatement: String
    /** Checks to see if the CREATE statement contains the REFERENCE keyword (denotes a FOREIGN KEY constraint) */
    val hasForeignKey: Boolean
        get() = createStatement.contains("REFERENCES")
    /**
     * Property showing the distinct table names that this table references
     *
     * If the table has no foreign keys, the set will be empty
     */
    val referencedTables: Set<String>
        get() = if (hasForeignKey) {
            "REFERENCES (public\\.)?(\\S+) "
                .toRegex()
                .findAll(createStatement)
                .map { it.groupValues[2] }
                .toSet()
        } else {
            setOf()
        }
}
