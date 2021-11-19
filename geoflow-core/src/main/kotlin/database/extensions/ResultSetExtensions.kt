package database.extensions

import formatInstantDateTime
import kotlinx.serialization.Serializable
import java.sql.ResultSet
import java.sql.Timestamp
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf

inline fun <reified T> ResultSet.rowToClass(): T {
    require(!isClosed) { "ResultSet is closed" }
    require(!isAfterLast) { "ResultSet has no more rows to return" }
    if (T::class.isData) {
        val constructor = T::class.constructors.first()
        val row = 0.until(metaData.columnCount).map {
            when (val current = getObject(it + 1)) {
                null -> current
                is Timestamp -> formatInstantDateTime(current.toInstant())
                else -> current
            }
        }.toTypedArray()
        return if (T::class.hasAnnotation<Serializable>()) {
            require(row.size == constructor.parameters.size - 2) {
                "Row size must match number of required constructor parameters"
            }
            constructor.call(Int.MAX_VALUE, *row, null)
        } else {
            require(row.size == constructor.parameters.size) {
                "Row size must match number of required constructor parameters"
            }
            constructor.call(*row)
        }
    }
    if (T::class.companionObject == null) {
        throw IllegalArgumentException("Type must have a companion object")
    }
    val resultSetType = typeOf<ResultSet>()
    val genericType = typeOf<T>()
    val companion = T::class.companionObject!!
    val function = companion.members.firstOrNull {
        val checks = it.parameters.size == 2 && resultSetType.isSubtypeOf(it.parameters[1].type)
        checks && genericType.isSubtypeOf(it.returnType.withNullability(genericType.isMarkedNullable))
    }
    if (function != null) {
        val instance = companion.objectInstance ?: companion.java.getDeclaredField("INSTANCE")
        return function.call(instance, this) as T
    }
    if (metaData.columnCount != 1) {
        throw IllegalArgumentException("Fallback to extract single value from result set failed since columnCount != 1")
    }
    return getObject(1) as T
}
