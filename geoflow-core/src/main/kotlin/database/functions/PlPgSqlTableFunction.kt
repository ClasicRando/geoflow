package database.functions

import database.DatabaseConnection
import kotlin.jvm.Throws
import kotlin.reflect.KClass

open class PlPgSqlTableFunction(
    val name: String,
    val parameterTypes: List<KClass<out Any>>
) {
    @Throws(IllegalArgumentException::class)
    fun call(vararg params: Any): List<Map<String, Any?>> {
        if (params.size != parameterTypes.size) {
            throw IllegalArgumentException("Expected ${parameterTypes.size} params, got ${params.size}")
        }
        val paramTypePairs = params.map { it::class } zip parameterTypes
        val paramMismatch = paramTypePairs.firstOrNull { (param, pType) -> param != pType }
        if (paramMismatch != null) {
            throw IllegalArgumentException("Expected param type ${paramMismatch.first}, got ${paramMismatch.second}")
        }
        return DatabaseConnection.database.useConnection { connection ->
            connection
                .prepareStatement("select * from $name(${"?".repeat(params.size)})")
                .apply {
                   params.forEachIndexed { index, obj ->
                       setObject(index + 1, obj)
                   }
                }
                .executeQuery()
                .use { rs ->
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