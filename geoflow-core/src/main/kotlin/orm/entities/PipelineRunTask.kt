package orm.entities

import org.ktorm.entity.Entity
import java.time.Instant

interface PipelineRunTask: Entity<PipelineRunTask> {
    val pipelineRunTaskId: Long
    val runId: Long
    var taskStart: Instant?
    var taskCompleted: Instant?
    val task: Task
    var taskMessage: String?
    var runTaskOrder: Int
    var taskStatus: TaskStatus
}