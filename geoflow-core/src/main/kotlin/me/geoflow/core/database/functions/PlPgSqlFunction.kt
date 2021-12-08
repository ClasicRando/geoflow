package me.geoflow.core.database.functions

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.extensions.queryFirstOrNull
import java.sql.Connection
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

/**
 * Base implementation of plpgsql functions.
 *
 * Requires a [name] for the function call and list of [parameterTypes] to verify that the params provided to the call
 * match the function signature in the database.
 *
 * The class also requires subclasses to provide the function create code and any inner functions called for rebuilding
 * the function in future databases
 *
 * @param name stored function name
 * @param parameterTypes types of parameters provided to the stored table function
 */
abstract class PlPgSqlFunction(
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
    protected inline fun <reified T> call(connection: Connection, vararg params: Any?): T {
        require(params.size == parameterTypes.size) {
            "Expected ${parameterTypes.size} params, got ${params.size}"
        }
        val paramMismatch = params.zip(parameterTypes).firstOrNull { (param, pType) ->
            if (param == null) {
                !pType.isMarkedNullable
            } else {
                !param::class.createType().isSubtypeOf(pType)
            }
        }
        if (paramMismatch != null) {
            throw IllegalArgumentException("Expected param type ${paramMismatch.first}, got ${paramMismatch.second}")
        }
        val sql = "select $name(${"?,".repeat(params.size).trim(',')})"
        return connection.queryFirstOrNull(sql = sql, *params)
            ?: throw NoRecordAffected(name, "plpgsql function $name did not return anything")
    }
}
