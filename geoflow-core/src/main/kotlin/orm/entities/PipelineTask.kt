package orm.entities

import org.ktorm.entity.Entity

interface PipelineTask: Entity<PipelineTask> {
    val pipelineId: Long
    val taskId: Long
}