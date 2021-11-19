package database.extensions

import formatInstantDateTime
import kotlinx.serialization.Serializable
import requireState
import java.sql.ResultSet
import java.sql.Timestamp
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf

/**
 * Returns an instance of the provided type [T] using the current [ResultSet] row.
 *
 * The row is transformed into [T] when the type meets 1 of 3 conditions:
 * - [T] is a data class whose parameters match the collected row form the [ResultSet]
 * - [T] has a static function in the companion object that accepts a [ResultSet] that is used to extract and generate
 * and instance of [T]
 * - [T] is a primitive type that can be extracted from the first and only value in the [ResultSet] row
 *
 * @throws IllegalArgumentException when:
 * - the [ResultSet] is closed or after last
 * - the [ResultSet] is after last
 * - [T] does not have a companion object (non-dataclass [T])
 * @throws IllegalStateException when:
 * - the extracted row's parameters do not match the count or type expected by the dataclass
 * - [T] is a primitive (or failed other requirements) and the row has more than 1 value
 * @throws TypeCastException when [T] is a primitive (or failed other requirement) and the object extracted is not the
 * type expected
 */
@Suppress("SpreadOperator")
inline fun <reified T> ResultSet.rowToClass(): T {
    require(!isClosed) { "ResultSet is closed" }
    require(!isAfterLast) { "ResultSet has no more rows to return" }
    if (T::class.isData) {
        val constructor = T::class.constructors.first()
        val row = buildList {
            for (i in 0 until metaData.columnCount) {
                val item = when (val current = getObject(i + 1)) {
                    null -> current
                    is Timestamp -> formatInstantDateTime(current.toInstant())
                    else -> current
                }
                add(item)
            }
        }.toTypedArray()
        return if (T::class.hasAnnotation<Serializable>()) {
            requireState(row.size == constructor.parameters.size - 2) {
                "Row size must match number of required constructor parameters"
            }
            constructor.call(Int.MAX_VALUE, *row, null)
        } else {
            requireState(row.size == constructor.parameters.size) {
                "Row size must match number of required constructor parameters"
            }
            constructor.call(*row)
        }
    }
    requireNotNull(T::class.companionObject) { "Type must have a companion object" }
    val resultSetType = typeOf<ResultSet>()
    val genericType = typeOf<T>()
    val companion = T::class.companionObject!!
    val function = companion.members.firstOrNull {
        val checks = it.parameters.size == 2 && resultSetType.isSubtypeOf(it.parameters[1].type)
        checks && genericType.isSubtypeOf(it.returnType.withNullability(genericType.isMarkedNullable))
    }
    return if (function != null) {
        val instance = companion.objectInstance ?: companion.java.getDeclaredField("INSTANCE")
        function.call(instance, this) as T
    } else {
        requireState(metaData.columnCount != 1) {
            "Fallback to extract single value from result set failed since columnCount != 1"
        }
        getObject(1) as T
    }
}

/**
 * Returns a generic type [T] using the [ResultSet]'s first row if available. Returns null if the [ResultSet] is null or
 * the [ResultSet] has no rows.
 *
 * @throws IllegalArgumentException when the [ResultSet] has already been initialized
 */
inline fun <T> ResultSet?.useFirstOrNull(block: (ResultSet) -> T): T? = use {
    return when {
        this == null -> null
        next() -> {
            require(!isClosed) { "ResultSet is closed" }
            require(isBeforeFirst) { "ResultSet has already been initialized" }
            block(this)
        }
        else -> null
    }
}

/**
 * Returns all rows of a [ResultSet] as a transformed type [T] using [rowToClass] as the transformation mechanism
 *
 * @throws IllegalArgumentException when:
 * - [ResultSet] is closed or already initialized
 * - [rowToClass] throws an exception
 * @throws IllegalStateException see [rowToClass]
 * @throws TypeCastException see [rowToClass]
 */
inline fun <reified T> ResultSet.collectRows(): List<T> {
    require(!isClosed) { "ResultSet is closed" }
    require(isBeforeFirst) { "ResultSet has already been initialized" }
    return buildList {
        while (next()) {
            add(rowToClass())
        }
    }
}
