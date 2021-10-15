package tasks

import database.DatabaseConnection
import mu.KotlinLogging
import orm.enums.TaskStatus
import orm.tables.PipelineRunTasks
import java.time.Instant

abstract class SystemTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    protected val logger = KotlinLogging.logger {}
    private val message = StringBuilder()

    abstract suspend fun run()

    protected fun messageAppend(text: String) {
        message.append(text)
    }

    protected fun messageAppend(func: () -> String) {
        message.append(func())
    }

    protected fun setMessage(text: String) {
        message.clear()
        message.append(text)
    }

    protected fun setMessage(func: () -> String) {
        setMessage(func())
    }

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
                TaskResult.Success(message.toString().takeIf { it.isNotBlank() })
            }.getOrElse { t ->
                TaskResult.Error(t)
            }
        }
    }
}