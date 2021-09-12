package tasks

import database.DatabaseConnection
import orm.tables.PipelineRunTasks
import java.time.Instant

abstract class SystemTask(pipelineRunTaskId: Long): PipelineTask(pipelineRunTaskId) {

    abstract suspend fun run()

    override suspend fun runTask() {
        DatabaseConnection.database.useTransaction {
            val taskRecord = PipelineRunTasks.reserveRecord(pipelineRunTaskId)
            taskRecord.taskStart = Instant.now()
            taskRecord.taskRunning = true
            taskRecord.taskComplete = false
            taskRecord.taskCompleted = null
            taskRecord.flushChanges()
            run()
            taskRecord.taskRunning = false
            taskRecord.taskComplete = true
            taskRecord.taskCompleted = Instant.now()
            taskRecord.flushChanges()
        }
    }
}