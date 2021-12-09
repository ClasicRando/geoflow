package tasks

import me.geoflow.core.database.Database
import me.geoflow.core.database.tables.PipelineRunTasks
import me.geoflow.core.database.tables.Tasks
import me.geoflow.core.tasks.SystemTask
import me.geoflow.core.tasks.UserTask
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import java.sql.Connection
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty
import kotlin.reflect.typeOf
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskChecks {

    data class UserTaskField(
        val fieldName: String,
        val fieldType: KType,
        val fieldValue: Any?,
        val taskName: String,
    )
    data class SystemTaskMethod(
        val returnType: KType,
        val parameters: List<KParameter>,
        val taskId: Long,
        val taskName: String,
    )

    private val userTasks: List<UserTaskField> by lazy {
        val config = ConfigurationBuilder()
            .forPackage("me/geoflow/tasks")
            .setScanners(Scanners.MethodsAnnotated)
        Reflections(config)
            .getFieldsAnnotatedWith(UserTask::class.java)
            .map { field ->
                val property = field.kotlinProperty!!
                val annotation = field.getAnnotation(UserTask::class.java)
                UserTaskField(
                    fieldName = field.name,
                    fieldType = property.returnType,
                    fieldValue = property.getter.call(),
                    taskName = annotation.taskName,
                )
            }
    }
    private val systemTasks: List<SystemTaskMethod> by lazy {
        val config = ConfigurationBuilder()
            .forPackage("me/geoflow/tasks")
            .setScanners(Scanners.MethodsAnnotated)
        Reflections(config)
            .getMethodsAnnotatedWith(SystemTask::class.java)
            .map { method ->
                val function = method.kotlinFunction
                val annotation = method.getAnnotation(SystemTask::class.java)
                SystemTaskMethod(
                    returnType = function!!.returnType,
                    parameters = function.parameters,
                    taskId = annotation.taskId,
                    taskName = annotation.taskName,
                )
            }
    }
    private val dbUserTasks: Map<Long, String> by lazy {
        Database.runWithConnectionBlocking {
            Tasks.getUserTasks(it)
        }.associate { it.taskId to it.name }
    }
    private val dbSystemTasks: Map<Long, String> by lazy {
        Database.runWithConnectionBlocking {
            Tasks.getSystemTasks(it)
        }.associate { it.taskId to it.name }
    }
    private val connectionType = typeOf<Connection>()
    private val prTaskType = typeOf<PipelineRunTasks.PipelineRunTask>()
    private val longType = typeOf<Long>()
    private val unitType = typeOf<Unit>()
    private val nullableStringType = typeOf<String>().withNullability(true)

    @Test
    fun checkCodebaseUserTasks() {
        val message = userTasks.mapNotNull { checkUserTask(it) }
        assertTrue(message.isEmpty(), message.joinToString(separator = "\n"))
    }

    private fun checkUserTask(userTaskField: UserTaskField): String? {
        if (userTaskField.fieldType.isSubtypeOf(longType)) {
            return "UserTask annotated fields must be of type `Long`, got ${userTaskField.fieldName}"
        }
        val taskId = userTaskField.fieldValue as Long
        val name = dbUserTasks[taskId]
        return when {
            name == null -> "Could not find codebase taskId = $taskId in the database"
            name != userTaskField.taskName -> {
                "For taskId = $taskId: Found name = '${userTaskField.taskName}', excepting $name"
            }
            else -> null
        }
    }

    @Test
    fun checkCodebaseSystemTasks() {
        val message = systemTasks.mapNotNull { checkSystemTask(it) }
        assertTrue(message.isEmpty(), message.joinToString(separator = "\n"))
    }

    private fun checkSystemTask(systemTaskMethod: SystemTaskMethod): String? {
        if (!isType(mainType = systemTaskMethod.returnType, unitType, nullableStringType)) {
            return "SystemTask methods much return `Unit` or `String?`, got ${systemTaskMethod.returnType}"
        }
        val taskId = systemTaskMethod.taskId
        val name = dbSystemTasks[taskId]
        return when {
            name == null  -> "Could not find codebase taskId = $taskId in the database"
            systemTaskMethod.parameters.size != 2 -> "TaskId = $taskId, system tasks require 2 parameters"
            systemTaskMethod.parameters[0].type.isSubtypeOf(connectionType) ->
                "TaskId = $taskId, system tasks require the first parameter to be a Connection"
            systemTaskMethod.parameters[1].type.isSubtypeOf(prTaskType)->
                "TaskId = $taskId, system tasks require the second parameter to be a PipelineRunTask type"
            else -> null
        }
    }

    private fun isType(mainType: KType, vararg types: KType): Boolean {
        return types.fold(false) { acc, kType ->
            acc || mainType.isSubtypeOf(kType)
        }
    }
}
