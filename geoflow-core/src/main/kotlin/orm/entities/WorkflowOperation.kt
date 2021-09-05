package orm.entities

import org.ktorm.entity.Entity

interface WorkflowOperation: Entity<WorkflowOperation> {
    val code: String
    val href: String
    val role: String
    val name: String
    val workflowOrder: Int
}