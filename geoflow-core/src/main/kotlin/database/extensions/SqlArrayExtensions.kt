package database.extensions

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
