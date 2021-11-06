package tasks

/**
 * User task to validate that the user has confirmed details of run instance
 */
class ConfirmRunInstance(pipelineRunTaskId: Long): UserTask(pipelineRunTaskId) {

    override val taskId: Long = 1
}