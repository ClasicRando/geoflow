package utils

import database.NoRecordAffected
import database.NoRecordFound
import database.tables.InternalUsers
import io.ktor.application.MissingApplicationFeatureException
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import java.sql.SQLException

/** alias describing API error information as array of objects */
typealias ApiErrors = List<Map<String, String?>>

/**
 * Extracts all errors thrown (includes suppressed exceptions) with connection to the throwable caught ([t]) and
 * converts each [Throwable] into a JSON object with the class name and [message][Throwable.message]
 */
fun throwableToResponseErrors(t: Throwable): ApiErrors {
    return t.suppressedExceptions.plus(t).map {
        val errorName = when (it) {
            is SQLException -> "sql_error"
            is SerializationException -> "json_decode_error"
            is IllegalArgumentException -> "argument_error"
            is IllegalStateException -> "internal_state_error"
            is InternalUsers.UserNotAdmin -> "admin_error"
            else -> {
                it::class.simpleName?.replace("([A-Z])([a-z])".toRegex()) { match ->
                    "${match.groupValues[1].lowercase()}_${match.groupValues[2]}"
                }?.lowercase()
            }
        }
        mapOf(
            "error_name" to errorName,
            "message" to it.message,
        )
    }
}

/** Returns an HTTP response code based upon the exception type of [t] */
fun errorCodeFromThrowable(t: Throwable): Int {
    return when (t) {
        is MissingApplicationFeatureException, is SerializationException, is IllegalArgumentException -> {
            HttpStatusCode.NotAcceptable.value
        }
        is NoRecordFound, is NoRecordAffected -> HttpStatusCode.NotFound.value
        else -> HttpStatusCode.InternalServerError.value
    }
}
