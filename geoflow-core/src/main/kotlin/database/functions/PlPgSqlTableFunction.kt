package database.functions

import database.DatabaseConnection
import rowToClass
import kotlin.jvm.Throws
import kotlin.reflect.KType
import kotlin.reflect.full.createType

/**
 * Base implementation of plpgsql table functions.
 *
 * Requires a [name] for the function call and list of [parameterTypes] to verify that the params provided to the call
 * match the function signature in the database.
 *
 * The class also requires subclasses to provide the function create code and any inner functions called for rebuilding
 * the function in future databases
 */
abstract class PlPgSqlTableFunction(
    val name: String,
    val parameterTypes: List<KType>,
) {
    /** Code called during the CREATE statement */
    abstract val functionCode: String
    /**
     * Function create statements for functions that are not called outside the main function but are required to be
     * created
     */
    abstract val innerFunctions: List<String>

    /**
     * Executes the function and transforms the ResultSet to a list of maps for each row returns. Provides that list as
     * the result.
     *
     * Validates that the number of params and the type of the params match the [parameterTypes] definition. Allows for
     * parameter definitions to include nullable types.
     *
     * @throws IllegalArgumentException when the params do not match the [parameterTypes] definition
     */
    @Throws(IllegalArgumentException::class)
    protected fun call(vararg params: Any?): List<Map<String, Any?>> {
        require(params.size == parameterTypes.size) {
            "Expected ${parameterTypes.size} params, got ${params.size}"
        }
        val paramMismatch = params.zip(parameterTypes).firstOrNull { (param, pType) ->
            if (param == null) {
                !pType.isMarkedNullable
            } else {
                param::class.createType() != pType
            }
        }
        if (paramMismatch != null) {
            throw IllegalArgumentException("Expected param type ${paramMismatch.first}, got ${paramMismatch.second}")
        }
        return DatabaseConnection.database.useConnection { connection ->
            connection
                .prepareStatement("select * from $name(${"?,".repeat(params.size).trim(',')})")
                .apply {
                   params.forEachIndexed { index, obj ->
                       setObject(index + 1, obj)
                   }
                }.use { statement ->
                    statement.executeQuery().use { rs ->
                        val columnNames = (1..rs.metaData.columnCount).map { rs.metaData.getColumnName(it) }
                        generateSequence {
                            if (rs.next()) {
                                columnNames.associateWith { name -> rs.getObject(name) }
                            } else {
                                null
                            }
                        }.toList()
                    }
                }
        }
    }

    /**
     * Executes the function and transforms the ResultSet to a list of maps for each row returns. Provides that list as
     * the result.
     *
     * Validates that the number of params and the type of the params match the [parameterTypes] definition. Allows for
     * parameter definitions to include nullable types.
     *
     * @throws IllegalArgumentException when the params do not match the [parameterTypes] definition
     */
    suspend inline fun <reified T> call2(vararg params: Any?): List<T> {
        require(params.size == parameterTypes.size) {
            "Expected ${parameterTypes.size} params, got ${params.size}"
        }
        val paramMismatch = params.zip(parameterTypes).firstOrNull { (param, pType) ->
            if (param == null) {
                !pType.isMarkedNullable
            } else {
                param::class.createType() != pType
            }
        }
        if (paramMismatch != null) {
            throw IllegalArgumentException("Expected param type ${paramMismatch.first}, got ${paramMismatch.second}")
        }
        val sql = "select * from $name(${"?,".repeat(params.size).trim(',')})"
        return DatabaseConnection.queryConnection { connection ->
            connection.prepareStatement(sql).apply {
                for (parameter in params.withIndex()) {
                    setObject(parameter.index + 1, parameter.value)
                }
            }.use { statement ->
                statement.executeQuery().use { rs ->
                    generateSequence {
                        if (rs.next()) rs.rowToClass<T>() else null
                    }.toList()
                }
            }
        }
    }
}