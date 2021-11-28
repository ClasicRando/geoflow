package api

import database.NoRecordAffected
import database.NoRecordFound
import io.ktor.application.ApplicationCall
import io.ktor.application.MissingApplicationFeatureException
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
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

/** Returns an HTTP response code based upon the exception type of [t] */
@Suppress("MagicNumber")
fun errorCodeFromThrowable(t: Throwable): Int {
    return when (t) {
        is MissingApplicationFeatureException, is IllegalArgumentException, is SerializationException -> 406
        is NoRecordFound, is NoRecordAffected -> 404
        else -> 500
    }
}

/** Utility function that summarizes api GET response objects using a getter lambda */
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

/**
 * Utility function that summarizes api POST response objects using a getter lambda to return an [ApiResponse] of the
 * specified type ([T])
 */
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

/**
 * Utility function that summarizes api POST response objects, receiving the body of the request into the specified type
 * ([B]) and returning an [ApiResponse] of the specified type ([T])
 */
inline fun <reified B: Any, reified R, reified T: ApiResponse.Success<R>> Route.apiPostReceive(
    path: String = "",
    crossinline func: suspend PipelineContext<Unit, ApplicationCall>.(B) -> T
) {
    post(path) {
        val response = runCatching {
            val requestBody = call.receive<B>()
            func(requestBody)
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

/**
 * Utility function that summarizes api PUT response objects, receiving the body of the request into the specified type
 * ([R]) and returning an [ApiResponse] of the specified type ([T])
 */
inline fun <reified R: Any, reified T: ApiResponse.Success<R>> Route.apiPutReceive(
    path: String = "",
    crossinline func: suspend PipelineContext<Unit, ApplicationCall>.(R) -> T
) {
    put(path) {
        val response = runCatching {
            val requestBody = call.receive<R>()
            func(requestBody)
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

/**
 * Utility function that summarizes api PATCH response objects, receiving the body of the request into the specified
 * type ([R]) and returning an [ApiResponse] of the specified type ([T])
 */
inline fun <reified R: Any, reified T: ApiResponse.Success<R>> Route.apiPatchReceive(
    path: String = "",
    crossinline func: suspend PipelineContext<Unit, ApplicationCall>.(R) -> T
) {
    patch(path) {
        val response = runCatching {
            val requestBody = call.receive<R>()
            func(requestBody)
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

/**
 * Utility function that summarizes api DELETE response objects returning an [ApiResponse] of the specified type ([T])
 */
inline fun <reified R: Any, reified T: ApiResponse.Success<R>> Route.apiDelete(
    path: String = "",
    crossinline func: suspend PipelineContext<Unit, ApplicationCall>.() -> T
) {
    delete(path) {
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
