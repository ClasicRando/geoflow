import tasks.UserTask
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatLocalDateDefault(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

fun formatInstantDefault(timestamp: Instant?) = timestamp
    ?.atZone(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""

fun formatInstantDateTime(timestamp: Instant?) = timestamp
    ?.atZone(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: ""

fun getUserPipelineTask(pipelineRunTaskId: Long, taskClassName: String): UserTask {
    return ClassLoader
        .getSystemClassLoader()
        .loadClass("tasks.$taskClassName")
        .getConstructor(Long::class.java)
        .newInstance(pipelineRunTaskId) as UserTask
}