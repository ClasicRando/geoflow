package tasks

import database.DatabaseConnection
import orm.tables.PipelineRunTasks
import java.time.Instant

abstract class UserTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    override suspend fun runTask() {
        val currentTime = Instant.now()
        DatabaseConnection.database.useTransaction {
            val taskRecord = PipelineRunTasks.reserveRecord(pipelineRunTaskId)
            taskRecord.taskStart = currentTime
            taskRecord.taskRunning = false
            taskRecord.taskComplete = true
            taskRecord.taskCompleted = currentTime
            taskRecord.flushChanges()
        }
    }
}