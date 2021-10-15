package tasks

import database.DatabaseConnection
import mu.KotlinLogging
import orm.enums.TaskStatus
import orm.tables.PipelineRunTasks
import java.time.Instant

abstract class SystemTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    protected val logger = KotlinLogging.logger {}

    abstract suspend fun run()

    override suspend fun runTask(): TaskResult {
        updateTask {
            taskStart = Instant.now()
            taskStatus = TaskStatus.Running
            taskCompleted = null
            taskMessage = null
        }
        return DatabaseConnection.database.useTransaction { transaction ->
            PipelineRunTasks.lockRecord(transaction, pipelineRunTaskId)
            runCatching {
                run()
                TaskResult.Success()
            }.getOrElse { t ->
                TaskResult.Error(t)
            }
        }
    }
}