package me.geoflow.core.tasks

import me.geoflow.core.database.Database
import me.geoflow.core.database.enums.TaskRunType
import me.geoflow.core.database.enums.TaskStatus
import me.geoflow.core.database.tables.PipelineRunTasks
import me.geoflow.core.database.tables.Tasks
import mu.KLogger
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import me.geoflow.core.utils.requireEmpty
import me.geoflow.core.utils.requireState
import java.time.Instant
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.typeOf

/** logger for tasks */
val taskLogger: KLogger = KotlinLogging.logger {}

/** Sealed interface to constrain Task info type */
sealed interface TaskInfo {
    /**
     * System task type with a single property of the function to be called
     *
     * @param function reflection view of a function to be used for a system task
     */
    data class SystemTaskInfo(val function: KFunction<*>) : TaskInfo
    /** Single class for user task type */
    object UserTaskInfo : TaskInfo
}

/**
 * lazy property that collects all annotated [SystemTask]s using reflection and queries the database for all user tasks
 * to properly register all runnable tasks.
 *
 * Checks to make sure none of the annotated functions repeat task IDs and the underlining function have the correct
 * return types.
 */
val tasks: Map<Long, TaskInfo> by lazy {
    val nullableStringType = typeOf<String>().withNullability(true)
    val unitType = typeOf<Unit>()
    val config = ConfigurationBuilder()
        .forPackage("me.geoflow.core.tasks")
        .setScanners(Scanners.MethodsAnnotated)
    val systemTasksList = Reflections(config)
        .getMethodsAnnotatedWith(SystemTask::class.java)
        .asSequence()
        .mapNotNull { it.kotlinFunction }
    val systemTasks = buildMap<Long, TaskInfo> {
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

    val userTasks = Database.runWithConnectionBlocking {
        Tasks.getUserTasks(it)
    }.associate { it.taskId to TaskInfo.UserTaskInfo }

    val intersectIds = userTasks.keys.intersect(systemTasks.keys)
    requireEmpty(intersectIds) { "TaskIds must not repeat: ${intersectIds.joinToString()}" }

    systemTasks + userTasks
}

/**
 * Returns a taskId for the provided function reference. Provided function must be annotated with [SystemTask]
 */
fun getTaskIdFromFunction(function: KFunction<*>): Long {
    require(function.annotations.any { it.annotationClass.java == SystemTask::class.java }) {
        "Function passed must annotated with @Task"
    }
    return tasks.entries.first { (_, info) -> info is TaskInfo.SystemTaskInfo && info.function == function }.key
}

/**
 * Task run function that executes the task associated with the [pipelineRunTaskId] provided. If the task is a
 * [SystemTask] then the linked function is run and does nothing if it's a [UserTask]. Runs the entire function in a
 * [runCatching] block, returning the appropriate [TaskResult].
 *
 * Starts by updating the [PipelineRunTasks]'s record to the running status with a start time of the current [Instant]
 * then proceeds to run the required logic in a transaction with the [PipelineRunTasks] record locked. Once the
 * task function has been completed (or nothing happens in the case of a [UserTask]), the transaction is completed
 * returning a [Success][TaskResult.Success] result. If an exception is thrown during any of these steps, the exception
 * is logged, and an [Error][TaskResult.Error] is returned with the provided [Throwable]
 */
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
            val message = when (prTask.task.taskRunTypeEnum) {
                TaskRunType.System -> {
                    taskInfo as TaskInfo.SystemTaskInfo
                    val result = if (taskInfo.function.isSuspend) {
                        taskInfo.function.callSuspend(connection, prTask)
                    } else {
                        taskInfo.function.call(connection, prTask)
                    }
                    result as? String?
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
