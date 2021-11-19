package database

import formatInstantDateTime
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import kotlin.reflect.full.*
import kotlin.reflect.typeOf

inline fun <reified T> ResultSet.rowToClass(): T {
    require(!isClosed) { "ResultSet is closed" }
    require(!isAfterLast) { "ResultSet has no more rows to return" }
    if (T::class.isData) {
        val constructor = T::class.constructors.first()
        val row = 0.until(metaData.columnCount).map {
            when (val current = getObject(it + 1)) {
                null -> current
                is Timestamp -> formatInstantDateTime(current.toInstant())
                else -> current
            }
        }.toTypedArray()
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
    if (T::class.companionObject == null) {
        throw IllegalArgumentException("Type must have a companion object")
    }
    val resultSetType = typeOf<ResultSet>()
    val genericType = typeOf<T>()
    val companion = T::class.companionObject!!
    val function = companion.members.firstOrNull {
        val checks = it.parameters.size == 2 && resultSetType.isSubtypeOf(it.parameters[1].type)
        checks && genericType.isSubtypeOf(it.returnType.withNullability(genericType.isMarkedNullable))
    }
    if (function != null) {
        val instance = companion.objectInstance ?: companion.java.getDeclaredField("INSTANCE")
        return function.call(instance, this) as T
    }
    if (metaData.columnCount != 1) {
        throw IllegalArgumentException("Fallback to extract single value from result set failed since columnCount != 1")
    }
    return getObject(1) as T
}

inline fun <reified T> Connection.submitQuery(
    sql: String,
    vararg parameters: Any?,
): List<T> {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.executeQuery().use { rs ->
            buildList {
                while (rs.next()) {
                    add(rs.rowToClass())
                }
            }
        }
    }
}

fun Connection.queryHasResult(
    sql: String,
    vararg parameters: Any?,
): Boolean {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.executeQuery().use {
            it.next()
        }
    }
}

inline fun <reified T> Connection.queryFirstOrNull(
    sql: String,
    vararg parameters: Any?,
): T? {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.executeQuery().useFirstOrNull { rs -> rs.rowToClass() }
    }
}

inline fun <T> ResultSet?.useFirstOrNull(block: (ResultSet) -> T): T? = use {
    return when {
        this == null -> null
        next() -> block(this)
        else -> null
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

fun Connection.call(
    procedureName: String,
    vararg parameters: Any?
) {
    prepareStatement("call $procedureName(${"?".repeat(parameters.size)})").use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.execute()
    }
}

fun Connection.runUpdate(
    sql: String,
    vararg parameters: Any?,
): Int {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.executeUpdate()
    }
}

fun Connection.executeNoReturn(
    sql: String,
    vararg parameters: Any?,
) {
    prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.execute()
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> Connection.runReturningOrNull(
    sql: String,
    vararg parameters: Any?,
): List<T> {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.execute()
        statement.resultSet.use { rs ->
            buildList {
                while (rs.next()) {
                    add(rs.getObject(1) as T)
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> Connection.runReturningFirstOrNull(
    sql: String,
    vararg parameters: Any?,
): T? {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.execute()
        statement.resultSet.useFirstOrNull { rs ->
            rs.getObject(1) as T
        }
    }
}

inline fun <reified T> java.sql.Array.getList(): List<T> {
    val arrayTyped = array as Array<*>
    return arrayTyped.map {
        when (it) {
            null -> throw IllegalStateException("SQL array item cannot be null")
            is T -> it
            else -> throw IllegalStateException("SQL array item must be of type ${typeOf<T>()}")
        }
    }
}

inline fun <reified T> java.sql.Array.getListWithNulls(): List<T?> {
    val arrayTyped = array as Array<*>
    return arrayTyped.map {
        if (it is T?) it else throw IllegalStateException("SQL array must be of type ${typeOf<T>()}")
    }
}
