package tasks

import database.DatabaseConnection
import org.ktorm.dsl.*
import orm.tables.SourceTables

class ValidateFirstPipeline(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 11

    override suspend fun run() {
        DatabaseConnection.database.run {
            val sourceTableCount = from(SourceTables)
                .select(count())
                .where { SourceTables.runId eq task.runId }
                .map { row -> row.getInt(1) }
                .first()
            if (sourceTableCount == 0)
                throw Exception("Source must have at least 1 file")
        }
    }

    companion object {
        const val taskId: Long = 11
    }
}