package tasks

class ConfirmRunInstance(pipelineRunTaskId: Long): UserTask(pipelineRunTaskId) {

    override val taskId: Long = 1
}