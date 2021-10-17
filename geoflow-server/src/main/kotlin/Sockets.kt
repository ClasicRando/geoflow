import database.startListener
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.util.*
import kotlin.collections.LinkedHashSet
import java.util.concurrent.atomic.AtomicLong

data class Connection(val session: DefaultWebSocketSession) {
    companion object {
        var lastId = AtomicLong(0)
    }
    val name = "user${lastId.getAndIncrement()}"
}

fun Route.publisher(path: String, channelName: String) {
    route(path) {
        val connections = Collections.synchronizedSet(LinkedHashSet<Connection>())
        var listener: Job? = null
        webSocket {
            val connection = Connection(this)
            connections += connection
            if (listener == null) {
                listener = startListener(channelName) { message ->
                    connections.forEach {
                        it.session.send(message)
                    }
                }
            }
            val listenerRunning = if (listener != null) "Listener is running" else "Listener is not running"
            send("Connected to pipelineRunTasks socket. $listenerRunning".trim())
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
                call.application.environment.log.info("pipelineRunTasks WebSocket job was cancelled")
            }  catch (t: Throwable) {
                call.application.environment.log.info("Exception during pipelineRunTasks WebSocket session", t)
            } finally {
                connections.remove(connection)
                if (connections.isEmpty()) {
                    listener?.cancelAndJoin()
                    listener = null
                }
            }
        }
    }
}