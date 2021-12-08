package me.geoflow.core.utils

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Formats [date] to the [ISO_LOCAL_DATE][DateTimeFormatter.ISO_LOCAL_DATE] */
fun formatLocalDateDefault(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

/** Formats [timestamp] to the [ISO_LOCAL_DATE][DateTimeFormatter.ISO_LOCAL_DATE_TIME] for the system default zone */
fun formatInstantDateTime(timestamp: Instant?): String = timestamp
    ?.atZone(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: ""

/**
 * Checks if the provided [collection] is empty and if so, throws an [IllegalStateException] with the lazy message
 */
inline fun <T> requireNotEmpty(collection: Collection<T>, lazyMessage: () -> Any) {
    if (collection.isEmpty()) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    }
}

/**
 * Checks if the provided [collection] is not empty and if so, throws an [IllegalStateException] with the lazy message
 */
inline fun <T> requireEmpty(collection: Collection<T>, lazyMessage: () -> Any) {
    if (collection.isNotEmpty()) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    }
}

/**
 * Returns a new array that applies a transformation to the receiver
 */
inline fun <reified T, reified R> Array<T>.mapToArray(transform: (T) -> R): Array<R> {
    return Array(this.size) { index ->
        transform(this[index])
    }
}

/**
 * Checks the required [value] and if the value is false, an [IllegalStateException] is thrown with the lazy message.
 */
inline fun requireState(value: Boolean, lazyMessage: () -> String) {
    if (!value) {
        val message = lazyMessage()
        throw IllegalStateException(message)
    }
}
