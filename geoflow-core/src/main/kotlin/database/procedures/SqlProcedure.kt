package database.procedures

import database.DatabaseConnection
import kotlin.jvm.Throws
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

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
    @Throws(IllegalArgumentException::class)
    protected fun call(vararg params: Any?) {
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
        DatabaseConnection
            .database
            .useConnection { connection ->
                connection.prepareStatement("call $name(${"?".repeat(params.size)})")
                    .apply {
                        params.forEachIndexed { index, obj ->
                            setObject(index + 1, obj)
                        }
                    }
                    .execute()
            }
    }
}