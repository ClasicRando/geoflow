package me.geoflow.api.sockets

import me.geoflow.core.database.startListener
import io.ktor.http.cio.websocket.send
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import me.geoflow.core.database.Database

/**
 * Publisher class to listen for postgresql notifications on a specific [channelName], passing along data to the
 * subscriber by serializing data obtained using the [jsonFetch] function.
 *
 * Publishers work with a 1 or more connections, using a [Mutex] lock to ensure shared state is handled properly. The
 * main function of the publisher is to spawn a listener on the specified [channel name][channelName] when at least 1
 * user is connected to the socket. All future connections do not spawn the listener. The listener uses a database
 * connection to run a LISTEN command and wait for notifications to arrive. Upon arrival, the listener runs a lambda
 * that locks the connection set and sends JSON data to the connections that are listening for the postgresql message.
 *
 * The listener is a simple launched coroutine so when no connections are currently active (ie connection set is empty),
 * the listener job is cancelled and the listener reference is set to null so the server knows to create a new listener
 * coroutine when future connections are initialized.
 */
class PostgresPublisher<T: Any>(
    /** Listen channel for the postgresql database */
    private val channelName: String,
    /** Serializer for encoding data into a JSON string */
    private val serializer: KSerializer<T>,
    /** Action provided to fetch data to be serialized and sent to the proper client connection */
    private val jsonFetch: (String, java.sql.Connection) -> T,
) {

    private val connections = LinkedHashSet<Connection>()
    private var listener: Job? = null
    private val mutex = Mutex()

    /**
     * Adds a connection to the connections set (while locking the mutex). If the listener is null, a new listener
     * coroutine is created to handle message propagation. After the mutex is released, the initial data needed by the
     * client to generated and sent to the client for initialization of its dataset.
     */
    suspend fun addConnection(connection: Connection): Unit = coroutineScope {
        mutex.withLock {
            connections.add(connection)
            if (listener == null) {
                listener = startListener(channelName) { message -> sendMessages(message) }
            }
        }
        Database.runWithConnectionAsync {
            val data = jsonFetch(connection.listenId, it)
            val json = Json.encodeToString(serializer, data)
            connection.session.send(json)
        }
    }

    /**
     * Sends the desired JSON data to clients listening for the provided [message]. Operation locks the mutex until
     * completion
     */
    private suspend inline fun sendMessages(message: String): Unit = mutex.withLock {
        for (c in connections.asSequence().filter { it.listenId == message }) {
            val json = Database.runWithConnection {
                val data = jsonFetch(message, it)
                Json.encodeToString(serializer, data)
            }
            c.session.send(json)
        }
    }

    /**
     * Removes the desired [connection] from the [connections] set and cancels the listener job if no other connections
     * to the publisher exist. Operation locks the mutex until completion
     */
    suspend fun closeConnection(connection: Connection): Unit = mutex.withLock {
        connections.remove(connection)
        if (connections.isEmpty()) {
            listener?.cancelAndJoin()
            listener = null
        }
    }

}
