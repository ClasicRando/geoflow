package tasks

import database.Database
import database.enums.TaskStatus
import database.tables.PipelineRunTasks
import java.time.Instant

/**
 * Base User task. All it does is set the record to complete and returns Success
 */
abstract class UserTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    override suspend fun runTask(): TaskResult {
        val currentTime = Instant.now()
        Database.runWithConnection {
            PipelineRunTasks.update(
                it,
                pipelineRunTaskId,
                taskStart = currentTime,
                taskStatus = TaskStatus.Complete,
                taskCompleted = currentTime
            )
        }
        return TaskResult.Success()
    }
}