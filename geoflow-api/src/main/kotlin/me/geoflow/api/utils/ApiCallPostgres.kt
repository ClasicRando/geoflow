package me.geoflow.api.utils

import io.ktor.application.ApplicationCall
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
import me.geoflow.core.database.Database
import java.sql.Connection

/**
 * Base api call that requires a postgresql database transaction. Allows for various [HttpMethod] types to be used for
 * an endpoint handler. Allows for a custom successful response generator with generic exception handling for
 * standardized error responses.
 */
inline fun <R, T: ApiResponse.Success<R>> Route.apiCallPostgres(
    httpMethod: HttpMethod,
    path: String = "",
    crossinline successfulResponse: suspend PipelineContext<Unit, ApplicationCall>.(Long, Connection) -> T
) {
    val action: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit = {
        val response = runCatching {
            Database.useTransaction { connection ->
                successfulResponse(
                    call.principal<JWTPrincipal>()!!.payload.getClaim("user_oid").asLong(),
                    connection,
                )
            }
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