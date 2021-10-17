import database.startListener
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.util.Collections.synchronizedSet
import kotlin.collections.LinkedHashSet

fun Route.publisher(path: String, channelName: String) {
    route(path) {
        val connections = synchronizedSet(LinkedHashSet<DefaultWebSocketSession>())
        var listener: Job? = null
        webSocket {
            connections += this
            if (listener == null) {
                listener = startListener(channelName) { message ->
                    connections.forEach {
                        it.send(message)
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
                connections.remove(this)
                if (connections.isEmpty()) {
                    listener?.cancelAndJoin()
                    listener = null
                }
            }
        }
    }
}