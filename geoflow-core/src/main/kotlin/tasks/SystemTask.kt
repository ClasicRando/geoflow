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

    protected fun addToMessage(text: String) {
        message.append(text)
    }

    protected fun setMessage(text: String) {
        message.clear().append(text)
    }

    override suspend fun runTask(): TaskResult {
        updateTask {
            set(it.taskStart, Instant.now())
            set(it.taskStatus, TaskStatus.Running)
            set(it.taskCompleted, null)
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