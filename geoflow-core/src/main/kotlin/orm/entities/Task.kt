package orm.entities

import org.ktorm.entity.Entity
import orm.enums.TaskRunType

interface Task: Entity<Task> {
    val taskId: Long
    val name: String
    val description: String
    val parentTaskId: Long?
    val state: String
    val parentTaskOrder: Int
    val taskRunType: TaskRunType
    val taskClassName: String?
}