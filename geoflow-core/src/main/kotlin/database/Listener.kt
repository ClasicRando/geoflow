package database

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import java.sql.SQLException

private val logger = KotlinLogging.logger {}

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
                        callback(notification.toString())
                    }
                }
                delay(1000)
            }
        } catch (e: SQLException) {
            logger.info("SQL error that has caused the listener to close", e)
        } catch(c: CancellationException) {
            logger.info("Job has been cancelled by parent scope")
        } catch (t: Throwable) {
            logger.info("Generic error that has caused the listener to close", t)
        }
    }
}