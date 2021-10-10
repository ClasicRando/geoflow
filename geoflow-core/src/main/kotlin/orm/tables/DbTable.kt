package orm.tables

import org.ktorm.entity.Entity
import org.ktorm.schema.Table

abstract class DbTable<E: Entity<E>>(tableName: String): Table<E>(tableName) {
    abstract val createStatement: String
}