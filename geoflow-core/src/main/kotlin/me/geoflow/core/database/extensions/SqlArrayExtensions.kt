package me.geoflow.core.database.extensions

import me.geoflow.core.database.composites.Composite
import me.geoflow.core.utils.requireNotEmpty
import java.sql.Connection
import kotlin.reflect.typeOf

/**
 * Returns all non-null items of an SQL Array with a type, [T], specified
 *
 * @throws IllegalStateException when an item is null or does not match the type [T]
 */
inline fun <reified T> java.sql.Array.getList(): List<T> {
    val arrayTyped = array as Array<*>
    return arrayTyped.map {
        when (it) {
            null -> throw IllegalStateException("SQL array item cannot be null")
            is T -> it
            else -> throw IllegalStateException("SQL array item must be of type ${typeOf<T>()}")
        }
    }
}

/**
 * Returns all items of an SQL Array with a type, [T], specified. Null items are allowed and cast to a nullable version
 * of the specified type.
 *
 * @throws IllegalStateException when an item does not match the type [T]
 */
inline fun <reified T> java.sql.Array.getListWithNulls(): List<T?> {
    val arrayTyped = array as Array<*>
    return arrayTyped.map {
        if (it is T?) it else throw IllegalStateException("SQL array must be of type ${typeOf<T>()}")
    }
}

/** */
inline fun <reified T: Composite> List<T>.getCompositeArray(connection: Connection): java.sql.Array {
    requireNotEmpty(this) { "Cannot get a composite array of an empty list" }
    return connection.createArrayOf(first().typeName, map { it.compositeValue }.toTypedArray())
}

/**
 * Returns an [Array][java.sql.Array] instance using the contents of this [List] that can be used perform sql operations
 */
inline fun <reified T> List<T>.getSqlArray(connection: Connection): java.sql.Array {
    val type = when(T::class) {
        String::class -> "text"
        Long::class -> "bigint"
        Int::class -> "int"
        Boolean::class -> "bool"
        else -> throw IllegalArgumentException("Array type ${T::class.java} is not supported")
    }
    return connection.createArrayOf(type, toTypedArray())
}
