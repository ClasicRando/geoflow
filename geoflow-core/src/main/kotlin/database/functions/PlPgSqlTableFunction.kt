package database.functions

import database.DatabaseConnection
import kotlin.jvm.Throws
import kotlin.reflect.KType
import kotlin.reflect.full.createType

open class PlPgSqlTableFunction(
    val name: String,
    val parameterTypes: List<KType>,
) {
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
}