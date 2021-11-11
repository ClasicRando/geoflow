package tasks

import database.tables.PipelineRunTasks
import database.enums.TaskStatus
import java.time.Instant

/**
 * Base User task. All it does is set the record to complete and returns Success
 */
abstract class UserTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    override suspend fun runTask(): TaskResult {
        val currentTime = Instant.now()
        PipelineRunTasks.update(
            pipelineRunTaskId,
            taskStart = currentTime,
            taskStatus = TaskStatus.Complete,
            taskCompleted = currentTime
        )
        return TaskResult.Success()
    }
}