package orm.tables

import org.ktorm.entity.Entity
import org.ktorm.schema.Table

/**
 * Base class for all tables defined. Extends [Table] and provides properties for building the DB from scratch.
 *
 * Enforces that subclasses have a CREATE statement that will be called in the build script. Also provides helper
 * properties used during the script to make sure foreign key dependencies are considered when ordering the CREATE
 * statements
 */
abstract class DbTable<E: Entity<E>>(tableName: String): Table<E>(tableName) {
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