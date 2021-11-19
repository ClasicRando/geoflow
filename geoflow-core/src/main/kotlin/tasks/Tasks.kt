package tasks

import database.Database
import database.enums.TaskRunType
import database.enums.TaskStatus
import database.tables.PipelineRunTasks
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import requireEmpty
import requireState
import java.time.Instant
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty
import kotlin.reflect.typeOf

val taskLogger = KotlinLogging.logger {}

sealed interface TaskInfo {
    data class SystemTaskInfo(val function: KFunction<*>) : TaskInfo
    object UserTaskInfo: TaskInfo
}

val tasks by lazy {
    val nullableStringType = typeOf<String>().withNullability(true)
    val unitType = typeOf<Unit>()
    val config = ConfigurationBuilder()
        .forPackage("tasks")
        .setScanners(Scanners.MethodsAnnotated)
    val systemTasksList = Reflections(config)
        .getMethodsAnnotatedWith(SystemTask::class.java)
        .asSequence()
        .mapNotNull { it.kotlinFunction }
    val systemTasks = buildMap {
        for (systemTask in systemTasksList) {
            val annotation = systemTask.annotations.first {
                it.annotationClass.java == SystemTask::class.java
            } as SystemTask
            val returnsNullableString = systemTask.returnType.isSubtypeOf(nullableStringType)
            val returnsUnit = systemTask.returnType.isSubtypeOf(unitType)
            requireState(returnsNullableString || returnsUnit) {
                "System task must return `String?` or `Unit`. Instead it returns ${systemTask.returnType}"
            }
            requireState(annotation.taskId !in this) { "TaskIds must not repeat: ${annotation.taskId}" }
            set(annotation.taskId, TaskInfo.SystemTaskInfo(systemTask))
        }
    }

    val longType = typeOf<Long>()
    val config2 = ConfigurationBuilder()
        .forPackage("tasks")
        .setScanners(Scanners.FieldsAnnotated)
    val userTasksList = Reflections(config2)
        .getFieldsAnnotatedWith(UserTask::class.java)
        .asSequence()
        .mapNotNull { it.kotlinProperty }
    val userTasks = buildMap {
        for (userTask in userTasksList) {
            requireState(userTask.isConst) { "User task property must be a const" }
            requireState(userTask.returnType.isSubtypeOf(longType)) { "User task property must be of type `Long`" }
            val taskId = userTask.getter.call() as Long
            requireState(taskId !in this) { "TaskIds must not repeat: $taskId" }
            set(taskId, TaskInfo.UserTaskInfo)
        }
    }

    val intersectIds = userTasks.keys.intersect(systemTasks.keys)
    requireEmpty(intersectIds) { "TaskIds must not repeat: ${intersectIds.joinToString()}" }

    systemTasks + userTasks
}

fun getTaskIdFromFunction(function: KFunction<*>): Long {
    require(function.annotations.any { it.annotationClass.java == SystemTask::class.java }) {
        "Function passed must annotated with @Task"
    }
    return tasks.entries.first { (_, info) -> info is TaskInfo.SystemTaskInfo && info.function == function }.key
}

suspend fun runTask(pipelineRunTaskId: Long): TaskResult {
    return runCatching {
        Database.runWithConnection {
            PipelineRunTasks.update(
                it,
                pipelineRunTaskId,
                taskStatus = TaskStatus.Running,
                taskStart = Instant.now(),
                taskCompleted = null,
            )
        }
        Database.useTransaction { connection ->
            val prTask = PipelineRunTasks.getWithLock(connection, pipelineRunTaskId)
            val taskInfo = tasks[prTask.task.taskId]
                ?: throw IllegalStateException("TaskId cannot be found in registered tasks")
            val message = when (prTask.task.taskRunType) {
                TaskRunType.System -> {
                    taskInfo as TaskInfo.SystemTaskInfo
                    val result = if (taskInfo.function.isSuspend) {
                        taskInfo.function.callSuspend(connection, prTask)
                    } else {
                        taskInfo.function.call(connection, prTask)
                    }
                    if (result is String?) {
                        result
                    } else {
                        null
                    }
                }
                TaskRunType.User -> { null }
            }
            TaskResult.Success(message)
        }
    }.getOrElse { t ->
        taskLogger.error("Error for $pipelineRunTaskId: ${t.message ?: "No message provided. See record"}")
        TaskResult.Error(t)
    }
}