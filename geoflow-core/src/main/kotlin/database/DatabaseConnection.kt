package database

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.context.dataSource
import com.github.michaelbull.jdbc.transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.commons.dbcp2.BasicDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException
import kotlin.coroutines.CoroutineContext

/**
 * Singleton to hold the primary database connection as a [DataSource][javax.sql.DataSource] and anything else linked to
 * that DataSource.
 */
object DatabaseConnection {
    @OptIn(ExperimentalSerializationApi::class)
    private val dataSource = BasicDataSource().apply {
        val json = File(System.getProperty("user.dir"), "db_config.json").readText()
        Json.decodeFromString<ConnectionProperties>(json).let { props ->
            driverClassName = props.className
            url = props.url
            username = props.username
            password = props.password
        }
        defaultAutoCommit = true
    }

    private val logger = KotlinLogging.logger {}
    val scope = CoroutineScope(Dispatchers.IO + CoroutineDataSource(dataSource))

    inline fun <reified T> CoroutineScope.useConnectionAsync(block: CoroutineScope.(Connection) -> T): T {
        val connection = if (coroutineContext.hasOpenConnection()) {
            coroutineContext.connection
        } else {
            coroutineContext.dataSource.connection
        }
        return connection.use {
            block(it)
        }
    }

    inline fun <reified T> CoroutineScope.useConnection(block: CoroutineScope.(Connection) -> T): T {
        val connection = if (coroutineContext.hasOpenConnection()) {
            coroutineContext.connection
        } else {
            coroutineContext.dataSource.connection
        }
        return connection.use {
            block(it)
        }
    }

    suspend inline fun <reified T> runWithConnection(crossinline func: (Connection) -> T): T {
        return withContext(scope.coroutineContext) {
            useConnection {
                func(it)
            }
        }
    }


    suspend inline fun <reified T> runWithConnectionAsync(crossinline func: suspend (Connection) -> T): T {
        return withContext(scope.coroutineContext) {
            useConnection {
                func(it)
            }
        }
    }

    suspend inline fun <T> useTransaction(
        crossinline func: suspend CoroutineScope.(Connection) -> T
    ): T {
        return withContext(scope.coroutineContext) {
            transaction {
                func(coroutineContext.connection)
            }
        }
    }

    fun CoroutineContext.hasOpenConnection(): Boolean {
        val connection = get(CoroutineConnection)?.connection
        return connection != null && !connection.isClosedCatching()
    }

    /**
     * Calls [isClosed][Connection.isClosed] on this [Connection] and returns its result, catching any [SQLException] that
     * was thrown then logging it and returning `true`.
     */
    private fun Connection.isClosedCatching(): Boolean {
        return try {
            isClosed
        } catch (ex: SQLException) {
            logger.warn(ex) { "Connection isClosedCatching check failed, assuming closed:" }
            true
        }
    }
}
