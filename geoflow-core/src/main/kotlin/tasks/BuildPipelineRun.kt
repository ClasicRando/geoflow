package tasks

import database.DatabaseConnection
import database.executeNoReturn
import database.runUpdate
import database.tables.PipelineRunTasks
import database.tables.PipelineRuns
import database.tables.SourceTables
import java.sql.Connection

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

    override suspend fun run(connection: Connection, task: PipelineRunTasks.PipelineRunTask) {
        val lastRun = PipelineRuns.lastRun(connection, task.pipelineRunTaskId)
        if (lastRun == null) {
            PipelineRunTasks.addTask(connection, pipelineRunTaskId, FirstPipelineDetected.taskId)
            PipelineRunTasks.addTask(connection, pipelineRunTaskId, ValidateFirstPipeline.taskId)
        } else {
            connection.executeNoReturn(
                sql = "DELETE FROM ${SourceTables.tableName} WHERE run_id = ?",
                task.runId,
            )
            val insertSql = """
                INSERT INTO ${SourceTables.tableName}(run_id,table_name,file_name,loader_type,qualified,encoding,
                                                      sub_table,file_id,url,comments,collect_type,delimiter)
                SELECT ?,table_name,file_name,loader_type,qualified,encoding,sub_table,file_id,url,comments,
                       collect_type,delimiter
                FROM   ${SourceTables.tableName}
                WHERE  run_id = ?
            """.trimIndent()
            connection.executeNoReturn(
                sql = insertSql,
                task.runId,
                lastRun,
            )
        }
    }

}