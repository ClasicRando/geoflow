package orm.entities

import org.ktorm.entity.Entity

interface PipelineTask: Entity<PipelineTask> {
    val pipelineTaskId: Long
    val pipelineId: Long
    val taskId: Long
    val parentTask: Long
    val parentTaskOrder: Int
}