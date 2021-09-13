package orm.entities

import org.ktorm.entity.Entity
import java.time.Instant

interface PipelineRunTask: Entity<PipelineRunTask> {
    val pipelineRunTaskId: Long
    val runId: Long
    var taskRunning: Boolean
    var taskComplete: Boolean
    var taskStart: Instant?
    var taskCompleted: Instant?
    val task: Task?
    val taskMessage: String?
}