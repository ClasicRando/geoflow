import database.NoRecordAffected
import database.NoRecordFound
import database.tables.InternalUsers
import io.ktor.application.ApplicationCall
import io.ktor.application.MissingApplicationFeatureException
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpMethod
import io.ktor.request.path
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
@Suppress("MagicNumber")
fun errorCodeFromThrowable(t: Throwable): Int {
    return when (t) {
        is MissingApplicationFeatureException, is SerializationException, is IllegalArgumentException -> 406
        is NoRecordFound, is NoRecordAffected -> 404
        else -> 500
    }
}

/** */
inline fun <R, T: ApiResponse.Success<R>> Route.apiCall(
    httpMethod: HttpMethod,
    path: String = "",
    crossinline func: suspend PipelineContext<Unit, ApplicationCall>.(Long) -> T
) {
    val action: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit = {
        val response = runCatching {
            func(call.principal<JWTPrincipal>()!!.payload.getClaim("user_oid").asLong())
        }.getOrElse { t ->
            call.application.log.error(call.request.path(), t)
            ApiResponse.Error(
                code = errorCodeFromThrowable(t),
                errors = throwableToResponseErrors(t)
            )
        }
        call.respond(response)
    }
    when (httpMethod) {
        HttpMethod.Get -> get(path) { action() }
        HttpMethod.Post -> post(path) { action() }
        HttpMethod.Put -> put(path) { action() }
        HttpMethod.Patch -> patch(path) { action() }
        HttpMethod.Delete -> delete(path) { action() }
    }
}
