package tasks

import orm.entities.PipelineRunTask
import orm.tables.PipelineRunTasks

abstract class PipelineTask(val pipelineRunTaskId: Long) {

    protected fun updateTask(action: PipelineRunTask.() -> Unit) {
        with(task) {
            action()
            flushChanges()
        }
    }
    protected val task: PipelineRunTask by lazy { PipelineRunTasks.getRecord(pipelineRunTaskId) }
    abstract val taskId: Long
    abstract suspend fun runTask(): Boolean
}