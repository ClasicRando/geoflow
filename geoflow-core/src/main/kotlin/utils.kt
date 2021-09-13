import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatInstantDefault(timestamp: Instant?) = timestamp
    ?.atZone(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""