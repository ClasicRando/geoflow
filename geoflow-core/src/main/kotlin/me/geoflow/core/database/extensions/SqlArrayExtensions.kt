package me.geoflow.core.database.extensions

import me.geoflow.core.database.composites.getCompositeName
import org.postgresql.util.PGobject
import java.sql.Connection
import kotlin.reflect.full.isSubclassOf
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

/**
 * Returns an [Array][java.sql.Array] instance using the contents of this [List] that can be used perform sql operations
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> List<T>.getSqlArray(connection: Connection): java.sql.Array {
    var isPgObject = false
    val type = when {
        String is T -> "text"
        Long is T -> "bigint"
        Int is T -> "int"
        Boolean is T -> "bool"
        T::class.isSubclassOf(PGobject::class) -> {
            isPgObject = true
            getCompositeName<T>()
        }
        else -> throw IllegalArgumentException("Array type ${T::class.java} is not supported")
    }
    return connection.createArrayOf(
        type,
        if (isPgObject) {
            map { (it as PGobject).value }.toTypedArray()
        } else toTypedArray()
    )
}
