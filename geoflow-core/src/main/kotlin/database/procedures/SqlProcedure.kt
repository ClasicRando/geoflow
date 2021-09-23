package database.procedures

import database.DatabaseConnection
import kotlin.jvm.Throws
import kotlin.reflect.KClass

open class SqlProcedure(
    val name: String,
    private val parameterTypes: List<KClass<out Any>>
) {
    @Throws(IllegalArgumentException::class)
    fun call(vararg params: Any) {
        if (params.size != parameterTypes.size) {
            throw IllegalArgumentException("Expected ${parameterTypes.size} params, got ${params.size}")
        }
        val paramTypePairs = params.map { it::class } zip parameterTypes
        val paramMismatch = paramTypePairs.firstOrNull { (param, pType) -> param != pType }
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