package tasks

class ValidateFirstPipeline(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 11

    override suspend fun run() {

    }

    companion object {
        const val taskId: Long = 11
    }
}