package api

/** alias describing API error information as array of objects */
typealias ApiErrors = List<Map<String, String?>>

/**
 * Extracts all errors thrown (includes suppressed exceptions) with connection to the throwable caught ([t]) and
 * converts each [Throwable] into a JSON object with the class name and [message][Throwable.message]
 */
fun throwableToResponseErrors(t: Throwable): ApiErrors {
    return t.suppressedExceptions.plus(t).map {
        mapOf(
            "class_name" to it::class.simpleName,
            "message" to it.message,
        )
    }
}
