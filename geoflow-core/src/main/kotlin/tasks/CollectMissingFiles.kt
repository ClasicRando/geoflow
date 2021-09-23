package tasks

class CollectMissingFiles(pipelineRunTaskId: Long): UserTask(pipelineRunTaskId) {

    override val taskId: Long = 5
    companion object {
        const val taskId: Long = 5
    }
}