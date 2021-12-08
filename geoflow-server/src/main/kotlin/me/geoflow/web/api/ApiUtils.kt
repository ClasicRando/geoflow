package me.geoflow.web.api

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.close
import io.ktor.http.contentType
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.util.getOrFail
import io.ktor.util.pipeline.PipelineContext
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.webSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.geoflow.web.session
import java.net.ConnectException

/**
 * Utility function to make a call to the API on behalf of the user.
 *
 * Calls to the specified API [endPoint] using the [httpMethod] specified, with a body (if needed) of type [B]. If
 * authentication is required (all non-login endpoints), the [apiToken] provided is used in the Authorization header.
 * The response from the API is then returned as the type [T].
 */
suspend inline fun <reified B, reified T> makeApiCall(
    endPoint: String,
    httpMethod: HttpMethod = HttpMethod.Get,
    content: B? = null,
    apiToken: String? = null
): T {
    return HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }.use { client ->
        client.request("http://localhost:8081$endPoint") {
            method = httpMethod
            content?.let { body = it }
            contentType(ContentType.Application.Json)
            apiToken?.let { header(HttpHeaders.Authorization, "Bearer $apiToken") }
        }
    }
}

/**
 * Loop to handle socket communication with the API. Launches a coroutine to pass forward each socket
 * [Frame][io.ktor.http.cio.websocket.Frame]. When the Web service socket connection is no longer active, this coroutine
 * is ended and the connection to the API socket is closed. Operation is wrapped with a complete try-catch so all
 * exception as handled and automatically end the coroutine
 */
private suspend fun DefaultClientWebSocketSession.socketLoop(serverSocket: DefaultWebSocketServerSession): Job {
    return launch {
        @Suppress("TooGenericExceptionCaught")
        try {
            for (frame in incoming) {
                if (!serverSocket.isActive) {
                    break
                }
                serverSocket.send(frame)
            }
        } catch (_: ClosedReceiveChannelException) {
        } catch (c: CancellationException) {
            if (!serverSocket.isActive) {
                serverSocket.call.application.environment.log.info("pipelineRunTasks WebSocket job was cancelled", c)
            }
        }  catch (t: Throwable) {
            if (!serverSocket.isActive) {
                serverSocket.call.application.environment.log.info(
                    "Exception during pipelineRunTasks WebSocket session",
                    t
                )
            }
        }
    }
}

private const val SOCKET_LOOP_CHECKUP = 500L

/**
 * Creates a socket connection with the API and runs a loop to pass along frames (see [socketLoop]).
 *
 * The socket operations starts with the creation of a coroutine that handles passing forward frames. A while loop is
 * then setup to check every 500ms to see if both the coroutine and underlining socket session with the web service
 * client are active. When either of those conditions are not met, the coroutine job and socket session are cleaned up
 * before exiting the function
 */
private suspend fun HttpClient.runSocket(
    apiPath: String,
    apiToken: String,
    socketSession: DefaultWebSocketServerSession,
) {
    webSocket(
        urlString = "ws://localhost:8081/api/$apiPath",
        request = {
            header(HttpHeaders.Authorization, "Bearer $apiToken")
        }
    ) {
        val job = socketLoop(socketSession)
        while (job.isActive && socketSession.isActive) {
            delay(SOCKET_LOOP_CHECKUP)
        }
        if (job.isActive) {
            job.cancelAndJoin()
        }
        if (socketSession.isActive) {
            socketSession.close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "API connection closed"))
        }
    }
}

/** Sets up a publisher that passes updates to data from API to the web service client */
fun Route.publisher(endPoint: String, path: String = "") {
    webSocket(path = path) {
        val socketSession = this
        val apiPath = Regex("\\{[^}]+}").findAll(path).fold(endPoint) { acc, current ->
            acc.replace(current.value, call.parameters.getOrFail(current.value.trim('{', '}')))
        }
        val session = call.session
        requireNotNull(session)
        runCatching {
            HttpClient(CIO){
                install(io.ktor.client.features.websocket.WebSockets)
            }.use { client ->
                client.runSocket(
                    apiPath = apiPath,
                    apiToken = session.apiToken,
                    socketSession = socketSession,
                )
            }
        }.getOrElse { t ->
            call.application.log.info(call.request.path(), t)
            when (t) {
                is ConnectException -> socketSession.close(
                    CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "API refused connection")
                )
                else -> socketSession.close(
                    CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Unknown error. Check log")
                )
            }
        }
    }
}

/** Singleton class representing an api call without a body provided */
object NoBody

/** Utility function that summarizes api GET response objects using a getter lambda */
inline fun <reified B> Route.apiCall(
    apiEndPoint: String,
    httpMethod: HttpMethod,
    path: String = "",
) {
    val action: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit = {
        val apiResponse = runCatching {
            val session = call.session
            requireNotNull(session)
            val apiPath = Regex("\\{[^}]+}").findAll(path).fold(apiEndPoint) { acc, current ->
                acc.replace(current.value, call.parameters.getOrFail(current.value.trim('{', '}')))
            }
            val content: B? = if (NoBody !is B) {
                call.receive()
            } else null
            makeApiCall<B, String>(
                endPoint = "/api/${apiPath}",
                httpMethod = httpMethod,
                content = content,
                apiToken = session.apiToken
            )
        }.getOrElse { t ->
            call.application.log.info(call.request.path(), t)
            """
                {
                    "error": "${t.message}"
                }
            """.trimIndent()
        }
        call.respond(apiResponse)
    }
    when (httpMethod) {
        HttpMethod.Get -> get(path) { action() }
        HttpMethod.Post -> post(path) { action() }
        HttpMethod.Put -> put(path) { action() }
        HttpMethod.Patch -> patch(path) { action() }
        HttpMethod.Delete -> delete(path) { action() }
    }
}
