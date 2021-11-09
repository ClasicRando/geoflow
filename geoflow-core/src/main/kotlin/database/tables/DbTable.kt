package database.tables

abstract class DbTable(val tableName: String) {
    /**
     * SQL create statement for the table
     */
    abstract val createStatement: String
    val hasForeignKey: Boolean
        get() = createStatement.contains("FOREIGN KEY")
    /**
     * Property showing the distinct table names that this table references
     *
     * If the table has no foreign keys, the set will be empty
     */
    val referencedTables: Set<String>
        get() = if (hasForeignKey) {
            "REFERENCES public\\.(\\S+) "
                .toRegex()
                .findAll(createStatement)
                .map { it.groupValues[1] }
                .toSet()
        } else {
            setOf()
        }
}
