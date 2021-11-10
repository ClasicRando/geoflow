import kotlinx.serialization.Serializable
import tasks.UserTask
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.createType
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

inline fun <reified T> ResultSet.rowToClass(): T {
    require(!isClosed) { "ResultSet is closed" }
    require(!isAfterLast) { "ResultSet has no more rows to return" }
    return if (T::class.isData) {
        val constructor = T::class.constructors.first()
        val row = 0.until(metaData.columnCount).map {
            when (val current = getObject(it + 1)) {
                null -> current
                is Timestamp -> formatInstantDateTime(current.toInstant())
                else -> current
            }
        }.toTypedArray()
        if (T::class.hasAnnotation<Serializable>()) {
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
    } else {
        requireNotNull(T::class.companionObject) { "Type must have a companion object" }
        val resultSetType = ResultSet::class.createType()
        val genericType = T::class.createType()
        val companion = T::class.companionObject!!
        val function = companion.members.firstOrNull {
            it.parameters.size == 1 && it.parameters[0].type == resultSetType && it.returnType == genericType
        } ?: throw IllegalArgumentException(
            "Type's companion object must have a function that accepts a ResultSet and returns an instance of the Type"
        )
        function.call(this) as T
    }
}

inline fun <T> ResultSet.useFirstOrNull(block: (ResultSet) -> T): T? = use {
    if (next()) {
        block(it)
    } else {
        null
    }
}

inline fun Connection.useMultipleStatements(sql: List<String>, block: (List<PreparedStatement>) -> Unit) {
    var exception: Throwable? = null
    var statements: List<PreparedStatement>? = null
    try {
        statements = sql.map { prepareStatement(it) }
        return block(statements)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        if (statements != null) {
            for (statement in statements) {
                if (exception == null) {
                    statement.close()
                } else {
                    try {
                        statement.close()
                    } catch (e: Throwable) {
                        exception.addSuppressed(e)
                    }
                }
            }
        }
    }
}
