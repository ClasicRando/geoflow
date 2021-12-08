package me.geoflow.api.sockets

import io.ktor.http.cio.websocket.DefaultWebSocketSession

/** Generic connection to a web socket endpoint */
data class Connection(
    /** web socket session for the connection */
    val session: DefaultWebSocketSession,
    /** message payload to listen for on the given postgresql notification socket */
    val listenId: String,
)
