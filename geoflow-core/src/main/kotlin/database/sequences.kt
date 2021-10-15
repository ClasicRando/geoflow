package database

import org.ktorm.database.Database
import org.ktorm.entity.sequenceOf
import orm.tables.PipelineRunTasks
import orm.tables.Roles
import orm.tables.SourceTableColumns
import orm.tables.SourceTables

val Database.roles get() = this.sequenceOf(Roles)
val Database.sourceTables get() = this.sequenceOf(SourceTables)
val Database.sourceTableColumns get() = this.sequenceOf(SourceTableColumns)
val Database.pipelineRunTasks get() = this.sequenceOf(PipelineRunTasks)