package tasks

import orm.enums.TaskStatus
import java.time.Instant

abstract class UserTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    override suspend fun runTask(): TaskResult {
        val currentTime = Instant.now()
        updateTask {
            taskStart = currentTime
            taskStatus = TaskStatus.Complete
            taskCompleted = currentTime
        }
        return TaskResult.Success()
    }
}