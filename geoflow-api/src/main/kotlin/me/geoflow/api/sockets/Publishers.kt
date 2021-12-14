package me.geoflow.api.sockets

import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import io.ktor.websocket.webSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.serializer

/**
 * Base publisher for a given [path] and LISTEN [channel name][channelName] for the database. Creates a publisher class
 * that handles all the WebSocket connections, message propagation and closing of connections and listener.
 */
inline fun <reified T: Any> Route.postgresPublisher(
    channelName: String,
    listenId: String,
    path: String = "",
    crossinline jsonFetch: (String, java.sql.Connection) -> T,
) {
    val publisher = PostgresPublisher(channelName, serializer()) { message, connection ->
        jsonFetch(message, connection)
    }
    webSocket(path = "$path/{${listenId}}") {
        val connection = Connection(session = this, listenId = call.parameters.getOrFail(listenId))
        publisher.addConnection(connection)
        @Suppress("TooGenericExceptionCaught")
        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> call.application.environment.log.info(frame.readText())
                    else -> call.application.environment.log.info("Other frame type")
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            val reason = closeReason.await()
            call.application.environment.log.info("pipelineRunTasks WebSocket session closed. $reason", e)
        } catch (c: CancellationException) {
            call.application.environment.log.info("pipelineRunTasks WebSocket job was cancelled", c)
        }  catch (t: Throwable) {
            call.application.environment.log.info("Exception during pipelineRunTasks WebSocket session", t)
        } finally {
            publisher.closeConnection(connection)
        }
    }
}
