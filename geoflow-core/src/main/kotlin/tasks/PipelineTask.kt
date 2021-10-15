package tasks

import database.DatabaseConnection
import org.ktorm.dsl.*
import orm.entities.PipelineRunTask
import orm.tables.PipelineRunTasks

abstract class PipelineTask(val pipelineRunTaskId: Long) {

    protected fun updateTask(action: AssignmentsBuilder.(PipelineRunTasks) -> Unit) {
        DatabaseConnection.database.update(PipelineRunTasks) {
            action(it)
            where { it.pipelineRunTaskId eq pipelineRunTaskId }
        }
    }
    protected val task: PipelineRunTask by lazy { PipelineRunTasks.getRecord(pipelineRunTaskId) }
    abstract val taskId: Long
    abstract suspend fun runTask(): TaskResult
}