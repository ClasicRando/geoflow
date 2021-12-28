package me.geoflow.core.database

import me.geoflow.core.database.Database.useConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import me.geoflow.core.database.extensions.executeNoReturn
import mu.KotlinLogging
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import java.sql.SQLException

private val logger = KotlinLogging.logger {}
private const val NOTIFICATION_CHECK_DELAY = 1000L

/**
 * Launches a coroutine that listens for notification from a specific channel and performs a callback operation when a
 * notification is received. Loops until the Job is cancelled and removes the listen command on the connection before
 * exiting the coroutine.
 *
 * Primarily used for web sockets and a pub/sub design to listen for table changes
 */
fun CoroutineScope.startListener(channelName: String, callback: suspend (String) -> Unit): Job {
    return launch(Database.scope.coroutineContext) {
        useConnection { connection ->
            val pgConnection: PGConnection = connection.unwrap(PGConnection::class.java)
            connection.executeNoReturn("LISTEN $channelName")
            @Suppress("TooGenericExceptionCaught")
            try {
                while (isActive) {
                    pgConnection.notifications?.let { notifications ->
                        for (notification: PGNotification in notifications) {
                            callback(notification.parameter)
                        }
                    }
                    delay(NOTIFICATION_CHECK_DELAY)
                }
            } catch (e: SQLException) {
                logger.info("SQL error that has caused the '$channelName' listener to close", e)
            } catch(c: CancellationException) {
                logger.info("'$channelName' listener Job has been cancelled by parent scope", c)
            } catch (t: Throwable) {
                logger.info("Generic error that has caused the '$channelName' listener to close", t)
            } finally {
                withContext(NonCancellable) {
                    if (!connection.isClosed) {
                        logger.info("Setting connection to unlisten to '$channelName'")
                        connection.executeNoReturn("UNLISTEN $channelName")
                    }
                }
            }
        }
    }
}
