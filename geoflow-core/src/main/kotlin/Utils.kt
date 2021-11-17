import java.time.*
import java.time.format.DateTimeFormatter

fun formatLocalDateDefault(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

fun formatInstantDefault(timestamp: Instant?) = timestamp
    ?.atZone(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""

fun formatInstantDateTime(timestamp: Instant?) = timestamp
    ?.atZone(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: ""

inline fun <T> requireNotEmpty(collection: Collection<T>, lazyMessage: () -> Any) {
    if (collection.isEmpty()) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    }
}

inline fun <reified T, reified R> Array<T>.mapToArray(transform: (T) -> R): Array<R> {
    return Array(this.size) { index ->
        transform(this[index])
    }
}
