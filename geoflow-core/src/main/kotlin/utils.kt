import orm.entities.Task
import tasks.SystemTask
import tasks.UserTask
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatInstantDefault(timestamp: Instant?) = timestamp
    ?.atZone(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""

fun formatInstantDateTime(timestamp: Instant?) = timestamp
    ?.atZone(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: ""

fun getUserPipelineTask(pipelineRunTaskId: Long, task: Task): UserTask {
    return ClassLoader
        .getSystemClassLoader()
        .loadClass("tasks.${task.taskClassName}")
        .getConstructor(Long::class.java)
        .newInstance(pipelineRunTaskId) as UserTask
}