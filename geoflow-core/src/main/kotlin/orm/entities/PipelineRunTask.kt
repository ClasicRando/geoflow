package orm.entities

import org.ktorm.entity.Entity
import java.time.Instant

interface PipelineRunTask: Entity<PipelineRunTask> {
    val pipelineRunTaskId: Long
    val runId: Long
    val taskRunning: Boolean
    val taskComplete: Boolean
    val taskStart: Instant
    val taskCompleted: Instant
    val taskId: Long?
    val taskMessage: String?
}