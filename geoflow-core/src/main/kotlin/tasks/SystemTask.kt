package tasks

import database.DatabaseConnection
import orm.enums.TaskStatus
import orm.tables.PipelineRunTasks
import java.time.Instant

abstract class SystemTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    abstract suspend fun run()

    override suspend fun runTask(): Boolean {
        updateTask {
            taskStart = Instant.now()
            taskStatus = TaskStatus.Running
            taskCompleted = null
            taskMessage = null
        }
        return DatabaseConnection.database.useTransaction { transaction ->
            PipelineRunTasks.lockRecord(transaction, pipelineRunTaskId)
            var message: String? = null
            try {
                run()
            } catch (t: Throwable) {
                message = "ERROR in Task: ${t.stackTraceToString()}"
            } finally {
                PipelineRunTasks.finishTransaction(
                    transaction,
                    pipelineRunTaskId,
                    message
                )
            }
            message == null
        }
    }
}