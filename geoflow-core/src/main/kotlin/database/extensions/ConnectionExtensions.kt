@file:Suppress("TooManyFunctions")
package database.extensions

import java.sql.Connection
import java.sql.PreparedStatement

/**
 * Returns a list of type [T] as a result of the [sql] query with statement [parameters] applied. Parameters are
 * provided as vararg parameters of [Any] type including null.
 *
 * Executes the statement built using the [sql] query and [parameters], using the [ResultSet][java.sql.ResultSet] to
 * collection rows using the provided generic type [T]. A list is built using each [ResultSet][java.sql.ResultSet] row
 * using [T] to infer the extraction methods from the row. See [rowToClass] for more details on row extraction.
 *
 * @throws java.sql.SQLException if the statement preparation or execution throw an exception
 * @throws IllegalStateException see [rowToClass]
 * @throws IllegalArgumentException see [rowToClass]
 * @throws TypeCastException see [rowToClass]
 */
inline fun <reified T> Connection.submitQuery(
    sql: String,
    vararg parameters: Any?,
): List<T> {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.executeQuery().use { rs ->
            rs.collectRows()
        }
    }
}

/**
 * Returns a list of type [T] as a result of the [sql] query with statement [parameters] applied. Parameters are
 * provided as a defined [List] of [Any] type including null.
 *
 * Executes the statement built using the [sql] query and [parameters], using the [ResultSet][java.sql.ResultSet] to
 * collection rows using the provided generic type [T]. A list is built using each [ResultSet][java.sql.ResultSet] row
 * using [T] to infer the extraction methods from the row. See [rowToClass] for more details on row extraction.
 *
 * @throws java.sql.SQLException if the statement preparation or execution throw an exception
 * @throws IllegalStateException see [rowToClass]
 * @throws IllegalArgumentException see [rowToClass]
 * @throws TypeCastException see [rowToClass]
 */
inline fun <reified T> Connection.submitQuery(
    sql: String,
    parameters: Collection<Any?>,
): List<T> {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.executeQuery().use { rs ->
            rs.collectRows()
        }
    }
}

/**
 * Returns a Boolean value denoting if the [sql] query provided returns any rows in the [ResultSet][java.sql.ResultSet]
 *
 * @throws java.sql.SQLException if the statement preparation or execution throw an exception
 */
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

/**
 * Returns the first result of the provided [sql] query, transforming the row into the desired type
 *
 * Performs a similar operation to [submitQuery] but returns a single nullable object of type [T]. The result will be
 * non-null if the query returns at least 1 result or null if the query returns nothing.
 *
 * @throws java.sql.SQLException when the query connection throws an exception
 * @throws IllegalArgumentException when [rowToClass]'s argument assertions fail
 * @throws IllegalStateException when [rowToClass]'s state assertions fail
 */
inline fun <reified T> Connection.queryFirstOrNull(
    sql: String,
    vararg parameters: Any?,
): T? {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.executeQuery().useFirstOrNull { it.rowToClass() }
    }
}

/**
 * Provide multiple [sql] queries to generate and handle the closing of subsequent statements. Statements are available
 * as the provided parameters of the lambda. Any exception thrown in the [block] or statement creation is rethrown
 * outside the function while still maintaining safe closing of the generated statements.
 */
@Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
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

/**
 * Shorthand of the call operation in Postgresql. Uses the [procedureName] to call the stored procedure with the
 * provided [parameters]
 *
 * @throws java.sql.SQLException if the statement preparation or execution throw an exception
 */
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

/**
 * Shorthand for running an update [sql] command using the provided [parameters]. Returns the update affected count
 *
 * @throws java.sql.SQLException if the statement preparation or execution throw an exception
 */
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

/**
 * Shorthand for running an update [sql] command using the provided [parameters]. Returns the update affected count
 *
 * @throws java.sql.SQLException if the statement preparation or execution throw an exception
 */
fun Connection.runUpdate(
    sql: String,
    parameters: Collection<Any?>,
): Int {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.executeUpdate()
    }
}

/**
 * Shorthand for running DML statements that do not return any counts (ie INSERT or DELETE). Uses the [sql] command
 * and [parameters] to execute the statement
 *
 * @throws java.sql.SQLException if the statement preparation or execution throw an exception
 */
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

/**
 * Returns a [List] of type [T] as the result of the RETURNING DML [sql] command provided. If the
 * [ResultSet][java.sql.ResultSet] is null or empty, an empty [List] is returned.
 *
 * @throws java.sql.SQLException if the statement preparation or execution throw an exception
 * @throws IllegalStateException see [rowToClass]
 * @throws IllegalArgumentException see [rowToClass]
 * @throws TypeCastException see [rowToClass]
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> Connection.runReturning(
    sql: String,
    vararg parameters: Any?,
): List<T> {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.execute()
        statement.resultSet?.use { rs ->
            rs.collectRows()
        } ?: listOf()
    }
}

/**
 * Returns an instance of type [T] as the first result of the RETURNING DML [sql] command provided. If the
 * [ResultSet][java.sql.ResultSet] is null or empty, null is returned.
 *
 * @throws java.sql.SQLException if the statement preparation or execution throw an exception
 * @throws IllegalStateException see [rowToClass]
 * @throws IllegalArgumentException see [rowToClass]
 * @throws TypeCastException see [rowToClass]
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> Connection.runReturningFirstOrNull(
    sql: String,
    vararg parameters: Any?,
): T? {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.execute()
        statement.resultSet.useFirstOrNull { rs ->
            rs.rowToClass()
        }
    }
}

/**
 * Returns an instance of type [T] as the first result of the RETURNING DML [sql] command provided. If the
 * [ResultSet][java.sql.ResultSet] is null or empty, null is returned.
 *
 * @throws java.sql.SQLException if the statement preparation or execution throw an exception
 * @throws IllegalStateException see [rowToClass]
 * @throws IllegalArgumentException see [rowToClass]
 * @throws TypeCastException see [rowToClass]
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> Connection.runReturningFirstOrNull(
    sql: String,
    parameters: Collection<Any?>,
): T? {
    return prepareStatement(sql).use { statement ->
        for (parameter in parameters.withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.execute()
        statement.resultSet.useFirstOrNull { rs ->
            rs.rowToClass()
        }
    }
}
