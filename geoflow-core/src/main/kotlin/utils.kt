import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import tasks.UserTask
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

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

inline fun <T> requireNotEmpty(collection: Collection<T>, lazyMessage: () -> Any) {
    if (collection.isEmpty()) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    }
}

inline fun <reified T> ResultSet.rowToDataClass(): T {
    require(!isClosed) { "ResultSet is closed" }
    require(!isAfterLast) { "ResultSet has no more rows to return" }
    require(T::class.isData) { "Type provided is not a data class" }
    val constructor = T::class.constructors.first()
    val row = 0.until(metaData.columnCount).map { getObject(it + 1) }.toTypedArray()
    return if (T::class.hasAnnotation<Serializable>()) {
        require(row.size == constructor.parameters.size - 2) {
            "Row size must match number of required constructor parameters"
        }
        constructor.call(Int.MAX_VALUE, *row, null)
    } else {
        require(row.size == constructor.parameters.size) {
            "Row size must match number of required constructor parameters"
        }
        constructor.call(*row)
    }
}
