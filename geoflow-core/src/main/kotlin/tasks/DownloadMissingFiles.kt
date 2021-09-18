package tasks

class DownloadMissingFiles(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    companion object {
        const val taskId: Long = 4
    }
    override val taskId: Long = 4

    override suspend fun run() {

    }
}