package tasks

import database.DatabaseConnection
import database.tables.SourceTables

/**
 * System task to validate the pipeline run has at least 1 [SourceTables] record entry
 */
class ValidateFirstPipeline(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 11

    override suspend fun run() {
        val sourceTableCount = DatabaseConnection.queryConnectionSingle { connection ->
            connection.prepareStatement(
                "SELECT COUNT(0) FROM ${SourceTables.tableName} WHERE run_id = ?"
            ).use { statement ->
                statement.setLong(1, task.runId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
        if (sourceTableCount == 0) {
            error("Source must have at least 1 file")
        }
    }

    companion object {
        const val taskId: Long = 11
    }
}