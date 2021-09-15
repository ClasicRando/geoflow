package tasks

import database.DatabaseConnection
import orm.entities.TaskStatus
import orm.tables.PipelineRunTasks
import java.time.Instant

abstract class UserTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    override suspend fun runTask() {
        val currentTime = Instant.now()
        DatabaseConnection.database.useTransaction {
            val taskRecord = PipelineRunTasks.reserveRecord(pipelineRunTaskId)
            taskRecord.taskStart = currentTime
            taskRecord.taskStatus = TaskStatus.Complete
            taskRecord.taskCompleted = currentTime
            taskRecord.flushChanges()
        }
    }
}