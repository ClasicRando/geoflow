@file:Suppress("TooManyFunctions")
package me.geoflow.core.database.extensions

import java.sql.Connection

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
        for (parameter in flattenParameters(*parameters).withIndex()) {
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
        for (parameter in flattenParameters(*parameters).withIndex()) {
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
        } ?: emptyList()
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
        for (parameter in flattenParameters(*parameters).withIndex()) {
            statement.setObject(parameter.index + 1, parameter.value)
        }
        statement.execute()
        statement.resultSet.useFirstOrNull { rs ->
            rs.rowToClass()
        }
    }
}

/**
 * Utility function to flatten a value into an [Iterable] of [IndexedValue]. If the value is not an [Iterable] then
 * the value is converted to an [IndexedValue] and returned as an [Iterable] with a single item
 */
private fun getParams(param: Any?): Iterable<IndexedValue<Any?>> {
    return if (param is Iterable<*>) {
        param.withIndex()
    }
    else {
        listOf(IndexedValue(0, param))
    }
}

/**
 * Runs a batch DML using the [sql] DML statement provided, treating each element of the [parameters][parameters]
 * iterable as a batch used for the statement. If the item type of [parameters] is not an [Iterable] then each item gets
 * treated as a single item in the batch.
 */
fun Connection.runBatchDML(
    sql: String,
    parameters: Iterable<Any?>,
): Int {
    return prepareStatement(sql).use { statement ->
        for (params in parameters) {
            for ((i, param) in getParams(params)) {
                statement.setObject(i + 1, param)
            }
            statement.addBatch()
        }
        statement.executeBatch()
    }.sum()
}

/** Flattens an iterable by passing each item to the sequence builder */
suspend fun SequenceScope<Any?>.extractParams(items: Iterable<*>) {
    for (item in items) {
        yield(item)
    }
}

/** Flattens an iterable by passing each item to the sequence builder */
suspend fun SequenceScope<Any?>.extractParams(items: Array<*>) {
    for (item in items) {
        yield(item)
    }
}

/** Returns a lazy sequence of params with iterable items flattened to single items */
fun flattenParameters(vararg params: Any?): Sequence<Any?> = sequence {
    for (param in params) {
        when (param) {
            is Iterable<*> -> extractParams(param)
            is Array<*> -> extractParams(param)
            else -> yield(param)
        }
    }
}
