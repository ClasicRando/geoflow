package tasks

import orm.enums.TaskStatus
import java.time.Instant

abstract class UserTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    override suspend fun runTask(): TaskResult {
        val currentTime = Instant.now()
        updateTask {
            set(it.taskStart, currentTime)
            set(it.taskStatus, TaskStatus.Complete)
            set(it.taskCompleted, currentTime)
        }
        return TaskResult.Success()
    }
}