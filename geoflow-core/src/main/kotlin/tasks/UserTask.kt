package tasks

import orm.enums.TaskStatus
import java.time.Instant

/**
 * Base User task. All it does is set the record to complete and returns Success
 */
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