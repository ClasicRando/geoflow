package database

import kotlinx.coroutines.*
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import java.sql.SQLException

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
            e.printStackTrace()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}