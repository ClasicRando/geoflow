package database.extensions

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

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
