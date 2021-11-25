package api

import database.NoRecordAffected
import database.NoRecordFound
import io.ktor.application.ApplicationCall
import io.ktor.application.MissingApplicationFeatureException
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.util.pipeline.PipelineContext
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

/** */
@Suppress("MagicNumber")
fun errorCodeFromThrowable(t: Throwable): Int {
    return when (t) {
        is MissingApplicationFeatureException, is IllegalArgumentException, is SerializationException -> 406
        is NoRecordFound, is NoRecordAffected -> 404
        else -> 500
    }
}

/** Utility function that summarizes api response for a single object into some parameters and a getter lambda */
suspend inline fun <reified T> PipelineContext<Unit, ApplicationCall>.apiResponseSingle(
    responseObject: String,
    errorMessage: String,
    func: PipelineContext<Unit, ApplicationCall>.() -> T
) {
    val response = runCatching {
        ApiResponse.SuccessSingle(
            responseObject = responseObject,
            payload = func()
        )
    }.getOrElse { t ->
        call.application.log.error(call.request.path(), t)
        ApiResponse.Error(
            code = errorCodeFromThrowable(t),
            errors = throwableToResponseErrors(t)
        )
    }
    call.respond(response)
}

/** Utility function that summarizes api get response objects using a getter lambda */
inline fun <reified R, reified T: ApiResponse.Success<R>> Route.apiGet(
    path: String = "",
    crossinline func: suspend PipelineContext<Unit, ApplicationCall>.() -> T
) {
    get(path) {
        val response = runCatching {
            func()
        }.getOrElse { t ->
            call.application.log.error(call.request.path(), t)
            ApiResponse.Error(
                code = errorCodeFromThrowable(t),
                errors = throwableToResponseErrors(t)
            )
        }
        call.respond(response)
    }
}

/** Utility function that summarizes api get response objects using a getter lambda */
inline fun <reified R, reified T: ApiResponse.Success<R>> Route.apiPost(
    path: String = "",
    crossinline func: suspend PipelineContext<Unit, ApplicationCall>.() -> T
) {
    post(path) {
        val response = runCatching {
            func()
        }.getOrElse { t ->
            call.application.log.error(call.request.path(), t)
            ApiResponse.Error(
                code = errorCodeFromThrowable(t),
                errors = throwableToResponseErrors(t)
            )
        }
        call.respond(response)
    }
}
