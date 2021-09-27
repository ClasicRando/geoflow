package tasks

import database.DatabaseConnection
import orm.enums.TaskStatus
import orm.tables.PipelineRunTasks
import java.time.Instant

abstract class SystemTask(
    pipelineRunTaskId: Long,
    val nextTaskId: Long? = null
): PipelineTask(pipelineRunTaskId) {

    abstract suspend fun run()

    override suspend fun runTask(): Boolean {
        return DatabaseConnection.database.useTransaction {
            val taskRecord = PipelineRunTasks.reserveRecord(pipelineRunTaskId)
            taskRecord.taskStart = Instant.now()
            taskRecord.taskStatus = TaskStatus.Running
            taskRecord.taskCompleted = null
            taskRecord.flushChanges()
            runCatching {
                run()
                taskRecord.taskStatus = TaskStatus.Complete
                taskRecord.taskCompleted = Instant.now()
                taskRecord.flushChanges()
                true
            }.getOrElse { t ->
                taskRecord.taskMessage = "ERROR: ${t.message}"
                taskRecord.taskStatus = TaskStatus.Failed
                taskRecord.taskCompleted = null
                taskRecord.flushChanges()
                false
            }
        }
    }
}