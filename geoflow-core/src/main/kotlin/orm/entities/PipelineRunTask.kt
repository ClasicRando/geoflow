package orm.entities

import org.ktorm.entity.Entity
import orm.enums.TaskStatus
import java.time.Instant

interface PipelineRunTask: Entity<PipelineRunTask> {
    val pipelineRunTaskId: Long
    val runId: Long
    val taskStart: Instant?
    val taskCompleted: Instant?
    val task: Task
    val taskMessage: String?
    val parentTaskId: Long
    val parentTaskOrder: Int
    val taskStatus: TaskStatus
    val workflowOperation: String
    val taskStackTrace: String?
}