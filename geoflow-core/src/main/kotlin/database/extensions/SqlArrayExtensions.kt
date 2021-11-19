package database.extensions

import kotlin.reflect.typeOf

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

inline fun <reified T> java.sql.Array.getListWithNulls(): List<T?> {
    val arrayTyped = array as Array<*>
    return arrayTyped.map {
        if (it is T?) it else throw IllegalStateException("SQL array must be of type ${typeOf<T>()}")
    }
}
