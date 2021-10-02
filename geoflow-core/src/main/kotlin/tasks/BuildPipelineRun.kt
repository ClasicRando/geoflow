package tasks

import database.DatabaseConnection
import database.sourceTables
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.forEach
import org.ktorm.support.postgresql.bulkInsert
import orm.tables.PipelineRunTasks
import orm.tables.PipelineRuns
import orm.tables.SourceTables

class BuildPipelineRun(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 1

    override suspend fun run() {
        val lastRun = PipelineRuns.lastRun(task)
        if (lastRun == null) {
            with(PipelineRunTasks) {
                addTask(task, FirstPipelineDetected.taskId)
                addTask(task, ValidateFirstPipeline.taskId)
            }
        } else {
            DatabaseConnection.database.run {
                delete(SourceTables) { it.runId eq task.runId }
                bulkInsert(SourceTables) {
                    sourceTables
                        .filter { it.runId eq lastRun }
                        .forEach { record ->
                            item {
                                set(SourceTables.runId, task.runId)
                                set(SourceTables.sourceTableName, record.tableName)
                                set(SourceTables.fileName, record.fileName)
                                set(SourceTables.loaderType, record.loaderType)
                                set(SourceTables.qualified, record.qualified)
                                set(SourceTables.encoding, record.encoding)
                                set(SourceTables.subTable, record.subTable)
                                set(SourceTables.fileId, record.fileId)
                                set(SourceTables.url, record.url)
                                set(SourceTables.comments, record.comments)
                                set(SourceTables.collectType, record.collectType)
                                set(SourceTables.delimiter, record.delimiter)
                            }
                        }
                }
            }
        }
    }

}