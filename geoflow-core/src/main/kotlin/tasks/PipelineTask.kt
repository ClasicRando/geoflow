package tasks

import database.DatabaseConnection
import org.ktorm.dsl.*
import orm.entities.PipelineRunTask
import orm.tables.PipelineRunTasks

/**
 * Base task for any data pipeline. Provides template for System and User tasks
 */
abstract class PipelineTask(val pipelineRunTaskId: Long) {

    /**
     * Subclass only utility function to update the table record associated with the task. Uses lambda to allow the user
     * to set any field they desire using assignment builder
     */
    protected fun updateTask(action: AssignmentsBuilder.(PipelineRunTasks) -> Unit) {
        DatabaseConnection.database.update(PipelineRunTasks) {
            action(it)
            where { it.pipelineRunTaskId eq pipelineRunTaskId }
        }
    }

    /**
     * Underlining entity in [PipelineRunTasks] retrieved in a lazy fashion
     *
     * Future Changes
     * --------------
     * - update to lazy async coroutine when JDBC coroutine is implemented
     */
    protected val task: PipelineRunTask by lazy { PipelineRunTasks.getRecord(pipelineRunTaskId) }
    abstract val taskId: Long

    /**
     * Base operation of this class. Task execution is facilitated through this function
     */
    abstract suspend fun runTask(): TaskResult
}