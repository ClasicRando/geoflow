package tasks

class FirstPipelineDetected(pipelineRunTaskId: Long): UserTask(pipelineRunTaskId) {
    override val taskId: Long = 10

    companion object {
        const val taskId: Long = 10
    }
}