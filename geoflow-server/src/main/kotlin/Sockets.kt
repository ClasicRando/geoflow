@file:Suppress("MatchingDeclarationName")

import database.startListener
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.send
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.route
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import io.ktor.websocket.webSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Generic connection to a web socket endpoint */
data class Connection(
    /** web socket session for the connection */
    val session: DefaultWebSocketSession,
    /** message payload to listen for on the given postgresql notification socket */
    val listenId: String,
)

/**
 * Base publisher for a given [path] and LISTEN [channel name][channelName] for the database.
 *
 * Publishers work with a 1 or more connections, using a [Mutex] lock to ensure shared state is handled properly. The
 * main function of the publisher is to spawn a listener on the specified [channel name][channelName] when at least 1
 * user is connected to the socket. All future connections do not spawn the listener. The listener uses a database
 * connection to run a LISTEN command and wait for notifications to arrive. Upon arrival, the listener runs a callback
 * that locks the connection set while the message is sent to all active connections.
 *
 * After connection to the WebSocket, the server listens to the user until the user closes the connection. When the
 * connection is closed, the server locks the connection set while the closed connection is removed from the set.
 *
 * The listener is a simple launched coroutine so when no connections are currently active (ie connection set is empty),
 * the listener job is cancelled and the listener reference is set to null so the server knows to create a new listener
 * coroutine when future connections are initialized.
 */
fun Route.publisher(path: String, channelName: String) {
    route(path) {
        val connections = LinkedHashSet<Connection>()
        var listener: Job? = null
        val publisherLock = Mutex()
        webSocket("/{param}") {
            val listenId = call.parameters.getOrFail("param")
            val connection = Connection(this, listenId)
            publisherLock.withLock {
                connections += connection
            }
            if (listener == null) {
                listener = startListener(channelName) { message ->
                    publisherLock.withLock {
                        connections
                            .asSequence()
                            .filter { it.listenId == message }
                            .forEach { it.session.send(message) }
                    }
                }
            }
            val listenerRunning = if (listener != null) "Listener is running" else "Listener is not running"
            send("Connected to pipelineRunTasks socket. $listenerRunning")
            @Suppress("TooGenericExceptionCaught")
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            call.application.environment.log.info(frame.readText())
                        }
                        else -> {
                            call.application.environment.log.info("Other frame type")
                        }
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                call.application.environment.log.info(
                    "pipelineRunTasks WebSocket session closed. ${closeReason.await()}",
                    e
                )
            } catch (c: CancellationException) {
                call.application.environment.log.info("pipelineRunTasks WebSocket job was cancelled", c)
            }  catch (t: Throwable) {
                call.application.environment.log.info("Exception during pipelineRunTasks WebSocket session", t)
            } finally {
                publisherLock.withLock {
                    connections.remove(connection)
                    if (connections.isEmpty()) {
                        listener?.cancelAndJoin()
                        listener = null
                    }
                }
            }
        }
    }
}

/**
 *
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> Route.publisher2(
    channelName: String,
    listenId: String,
    path: String = "",
    crossinline func: suspend (String) -> T,
) {
    val connections = LinkedHashSet<Connection>()
    var listener: Job? = null
    val publisherLock = Mutex()
    webSocket("$path/{${listenId}}") {
        val connection = Connection(this, call.parameters.getOrFail(listenId))
        publisherLock.withLock {
            connections += connection
        }
        if (listener == null) {
            listener = startListener(channelName) { message ->
                publisherLock.withLock {
                    for (c in connections) {
                        if (c.listenId == message) {
                            c.session.send(Json.encodeToString(func(message)))
                        }
                    }
                }
            }
        }
        val listenerRunning = if (listener != null) "Listener is running" else "Listener is not running"
        send("Connected to pipelineRunTasks socket. $listenerRunning")
        @Suppress("TooGenericExceptionCaught")
        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        call.application.environment.log.info(frame.readText())
                    }
                    else -> {
                        call.application.environment.log.info("Other frame type")
                    }
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            call.application.environment.log.info(
                "pipelineRunTasks WebSocket session closed. ${closeReason.await()}",
                e
            )
        } catch (c: CancellationException) {
            call.application.environment.log.info("pipelineRunTasks WebSocket job was cancelled", c)
        }  catch (t: Throwable) {
            call.application.environment.log.info("Exception during pipelineRunTasks WebSocket session", t)
        } finally {
            publisherLock.withLock {
                connections.remove(connection)
                if (connections.isEmpty()) {
                    listener?.cancelAndJoin()
                    listener = null
                }
            }
        }
    }
}
