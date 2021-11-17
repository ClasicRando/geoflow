package database.procedures

import database.call
import java.sql.Connection
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

abstract class SqlProcedure(
    val name: String,
    private val parameterTypes: List<KType>
) {
    /** Code called during the CREATE statement */
    abstract val code: String

    /**
     * Executes the procedure with the given params array
     *
     * Validates that the number of params and the type of the params match the [parameterTypes] definition. Allows for
     * parameter definitions to include nullable types.
     *
     * @throws IllegalArgumentException when the params do not match the [parameterTypes] definition
     */
    protected fun call(connection: Connection, vararg params: Any?) {
        require(params.size == parameterTypes.size) {
            "Expected ${parameterTypes.size} params, got ${params.size}"
        }
        val paramMismatch = params.zip(parameterTypes).firstOrNull { (param, pType) ->
            if (param == null) {
                !pType.isMarkedNullable
            } else {
                param::class.createType().isSubtypeOf(pType)
            }
        }
        if (paramMismatch != null) {
            throw IllegalArgumentException("Expected param type ${paramMismatch.first}, got ${paramMismatch.second}")
        }
        connection.call(name, *params)
    }
}