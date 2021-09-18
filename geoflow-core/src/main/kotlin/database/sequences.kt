package database

import org.ktorm.database.Database
import org.ktorm.entity.sequenceOf
import orm.tables.Roles
import orm.tables.SourceTables

val Database.roles get() = this.sequenceOf(Roles)
val Database.sourceTables get() = this.sequenceOf(SourceTables)