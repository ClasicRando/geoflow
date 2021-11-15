package tasks

import database.Database
import database.enums.TaskRunType
import database.enums.TaskStatus
import database.tables.PipelineRunTasks
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import java.time.Instant
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty

val taskLogger = KotlinLogging.logger {}

sealed interface TaskInfo {
    data class SystemTaskInfo(val function: KFunction<*>, val parameters: List<KParameter>) : TaskInfo
    object UserTaskInfo: TaskInfo
}

val tasks by lazy {
    val nullableStringType = String::class.createType(nullable = true)
    val unitType = Unit::class.createType()
    val config = ConfigurationBuilder()
        .forPackage("tasks")
        .setScanners(Scanners.MethodsAnnotated)
    val systemTasks = Reflections(config)
        .getMethodsAnnotatedWith(SystemTask::class.java)
        .asSequence()
        .mapNotNull { it.kotlinFunction }
        .filter { it.returnType == nullableStringType || it.returnType == unitType }
        .associate { function ->
            val annotation = function.annotations.first { it.annotationClass.java == SystemTask::class.java } as SystemTask
            @Suppress("UNCHECKED_CAST")
            annotation.taskId to TaskInfo.SystemTaskInfo(function, function.parameters)
        }
    val longType = Long::class.createType()
    val config2 = ConfigurationBuilder()
        .forPackage("tasks")
        .setScanners(Scanners.FieldsAnnotated)
    val userTasks = Reflections(config2)
        .getFieldsAnnotatedWith(UserTask::class.java)
        .asSequence()
        .mapNotNull { it.kotlinProperty }
        .filter { it.isConst && it.returnType == longType }
        .associate { property ->
            property.getter.call() as Long to TaskInfo.UserTaskInfo
        }
    systemTasks + userTasks
}

fun getTaskIdFromFunction(function: KFunction<*>): Long {
    require(function.annotations.any { it.annotationClass.java == SystemTask::class.java }) {
        "Function passed must annotated with @Task"
    }
    return tasks.entries.first { (_, info) -> info is TaskInfo.SystemTaskInfo && info.function === function }.key
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