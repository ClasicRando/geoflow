package tasks

import database.DatabaseConnection
import mu.KotlinLogging
import orm.enums.TaskStatus
import orm.tables.PipelineRunTasks
import java.time.Instant

/**
 * Base System task. These tasks are instantiated in the worker application to perform operations.
 *
 * Adds some extra utility functions and a generic run function for performing operation custom to task
 */
abstract class SystemTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    protected val logger = KotlinLogging.logger {}
    /** Buildable message to be applied to the table record after runTask completion */
    private val message = StringBuilder()

    abstract suspend fun run()

    protected fun addToMessage(text: String) {
        message.append(text)
    }

    /** Clears current message text and appends the provided [text] */
    protected fun setMessage(text: String) {
        message.clear().append(text)
    }

    /**
     * Implementation of the abstract runTask function from parent class. Prepares task then runs task operation.
     *
     * Steps are as follows:
     * 1. Updates task to running state
     * 2. Locks record for future update while task is running
     * 3. Runs task operation, catching any throwable
     * 4. If no errors are thrown, return Success with message if not blank. If error throw, return error with throwable
     */
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
                logger.error("Error for $pipelineRunTaskId: ${t.message ?: "No message provided. See record"}")
                TaskResult.Error(t)
            }
        }
    }
}