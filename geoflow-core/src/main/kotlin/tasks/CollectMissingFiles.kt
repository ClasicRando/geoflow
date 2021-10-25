package tasks

/**
 * User task to remind the user to collect all files noted in the task message
 */
class CollectMissingFiles(pipelineRunTaskId: Long): UserTask(pipelineRunTaskId) {

    override val taskId: Long = 5
    companion object {
        const val taskId: Long = 5
    }
}