package tasks

/**
 * User task to notify the user that this data source has never been loaded before
 */
class FirstPipelineDetected(pipelineRunTaskId: Long): UserTask(pipelineRunTaskId) {
    override val taskId: Long = 10

    companion object {
        const val taskId: Long = 10
    }
}