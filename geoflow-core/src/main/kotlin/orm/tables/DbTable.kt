package orm.tables

import org.ktorm.entity.Entity
import org.ktorm.schema.Table

abstract class DbTable<E: Entity<E>>(tableName: String): Table<E>(tableName) {
    abstract val createStatement: String
    val hasForeignKey: Boolean
        get() = createStatement.contains("FOREIGN KEY")
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