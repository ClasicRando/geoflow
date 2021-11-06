package database

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import java.sql.SQLException

private val logger = KotlinLogging.logger {}

/**
 * Launches a coroutine that listens for notification from a specific channel and performs a callback operation when a
 * notification is received. Loops until the Job is cancelled and removes the listen command on the connection before
 * exiting the coroutine.
 *
 * Primarily used for web sockets and a pub/sub design to listen for table changes
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.startListener(channelName: String, callback: suspend (String) -> Unit) = launch {
    DatabaseConnection.database.useConnection { connection ->
        val pgConnection: PGConnection = connection.unwrap(PGConnection::class.java)
        connection.prepareStatement("LISTEN $channelName").use {
            it.execute()
        }
        try {
            while (isActive) {
                pgConnection.notifications?.let { notifications ->
                    for (notification: PGNotification in notifications) {
                        callback(notification.parameter)
                    }
                }
                delay(1000)
            }
        } catch (e: SQLException) {
            logger.info("SQL error that has caused the '$channelName' listener to close", e)
        } catch(c: CancellationException) {
            logger.info("'$channelName' listener Job has been cancelled by parent scope")
        } catch (t: Throwable) {
            logger.info("Generic error that has caused the '$channelName' listener to close", t)
        } finally {
            withContext(NonCancellable) {
                if (!connection.isClosed) {
                    logger.info("Setting connection to unlisten to '$channelName'")
                    connection.prepareStatement("UNLISTEN $channelName").use {
                        it.execute()
                    }
                }
            }
        }
    }
}