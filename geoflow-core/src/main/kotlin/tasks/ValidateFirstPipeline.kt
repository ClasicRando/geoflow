package tasks

import database.queryFirstOrNull
import database.tables.PipelineRunTasks
import database.tables.SourceTables
import java.sql.Connection

/**
 * System task to validate the pipeline run has at least 1 [SourceTables] record entry
 */
class ValidateFirstPipeline(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 11

    override suspend fun run(connection: Connection, task: PipelineRunTasks.PipelineRunTask) {
        val sourceTableCount = connection.queryFirstOrNull<Long>(
            sql = "SELECT COUNT(0) FROM ${SourceTables.tableName} WHERE run_id = ?",
            task.runId,
        ) ?: 0
        if (sourceTableCount == 0L) {
            error("Source must have at least 1 file")
        }
    }

    companion object {
        const val taskId: Long = 11
    }
}