package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.geoflow.core.database.enums.TaskRunType
import me.geoflow.core.database.tables.Tasks

/** Table record for [Tasks] */
@Suppress("unused")
@Serializable
data class Task (
    /** unique ID of the task */
    @SerialName("task_id")
    val taskId: Long,
    /** full name of the task */
    val name: String,
    /** description of the task functionality/intent */
    val description: String,
    /** intended workflow state that task is attached to */
    val state: String,
    /** string value of task run type */
    @SerialName("task_run_type")
    val taskRunType: String,
) {
    /** Enum value of task run type */
    val taskRunTypeEnum: TaskRunType get() = TaskRunType.valueOf(taskRunType)

    init {
        require(runCatching { TaskRunType.valueOf(taskRunType) }.isSuccess) {
            "string value passed for TaskRunType is not valid"
        }
    }

    companion object {
        /** SQL query used to generate the parent class */
        val sql: String = "SELECT task_id, name, description, state, task_run_type FROM ${Tasks.tableName}"
    }
}
