package tasks

import database.DatabaseConnection
import orm.tables.PipelineRunTasks
import java.time.Instant

abstract class PipelineTask {

    abstract val taskId: Long
    abstract val pipelineRunTaskId: Long
    abstract suspend fun task()

    suspend fun run() {
        DatabaseConnection.database.useTransaction {
            val taskRecord = PipelineRunTasks.reserveRecord(pipelineRunTaskId)
            taskRecord.taskStart = Instant.now()
            taskRecord.taskRunning = true
            taskRecord.taskComplete = false
            taskRecord.taskCompleted = null
            taskRecord.flushChanges()
            task()
            taskRecord.taskRunning = false
            taskRecord.taskComplete = true
            taskRecord.taskCompleted = Instant.now()
            taskRecord.flushChanges()
        }
    }
}