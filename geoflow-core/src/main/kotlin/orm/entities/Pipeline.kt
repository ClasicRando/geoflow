package orm.entities

import org.ktorm.entity.Entity

interface Pipeline: Entity<Pipeline> {
    val pipelineId: Long
    val name: String
    val workflowOperation: WorkflowOperation
}