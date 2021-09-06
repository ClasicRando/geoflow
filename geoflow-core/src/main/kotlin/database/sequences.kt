package database

import org.ktorm.database.Database
import org.ktorm.entity.sequenceOf
import orm.tables.Roles

val Database.roles get() = this.sequenceOf(Roles)