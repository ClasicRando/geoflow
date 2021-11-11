package tasks


/**
 * Base task for any data pipeline. Provides template for System and User tasks
 */
abstract class PipelineTask(val pipelineRunTaskId: Long) {

    abstract val taskId: Long

    /**
     * Base operation of this class. Task execution is facilitated through this function
     */
    abstract suspend fun runTask(): TaskResult
}