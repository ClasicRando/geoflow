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

/**
 * System task to build the initial pipeline run state.
 *
 * Checks for past runs for the data source. If past runs exist, copy forward the last run's source table data. If not,
 * then create 2 new child tasks:
 * 1. User task to remind the user to populate the source tables provided
 * 2. System task to validate that the run is ready to proceed
 */
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