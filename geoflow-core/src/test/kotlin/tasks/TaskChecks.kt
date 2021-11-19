package tasks

import database.Database
import database.tables.PipelineRunTasks
import database.tables.Tasks
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import kotlin.reflect.typeOf
import kotlin.test.assertFalse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskChecks {

    private val connectionType = typeOf<Connection>()
    private val prTaskType = typeOf<PipelineRunTasks.PipelineRunTask>()

    @Test
    fun checkTaskParameters() {
        val messages = mutableListOf<String>()
        var isFail = false
        for ((taskId, info) in tasks) {
            val message = checkTask(taskId, info)
            if (message != null) {
                isFail = true
                messages += message
            }
        }
        assertFalse(isFail, messages.joinToString(separator = "\n"))
    }

    private fun checkTask(taskId: Long, info: TaskInfo): String? {
        runBlocking {
            Database.runWithConnection {
                Tasks.getRecord(it, taskId)
            }
        } ?: return "TaskId = $taskId could not be found in the `tasks` table"
        return when (info) {
            is TaskInfo.SystemTaskInfo -> {
                when {
                    info.function.parameters.size != 2 -> "TaskId = $taskId, system tasks require 2 parameters"
                    info.function.parameters[0].type != connectionType ->
                        "TaskId = $taskId, system tasks require the first parameter to be a Connection"
                    info.function.parameters[1].type != prTaskType ->
                        "TaskId = $taskId, system tasks require the second parameter to be a PipelineRunTask type"
                    else -> null
                }
            }
            is TaskInfo.UserTaskInfo -> {
                null
            }
        }
    }
}