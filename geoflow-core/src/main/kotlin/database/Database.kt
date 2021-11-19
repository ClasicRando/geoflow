package database

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.context.dataSource
import com.github.michaelbull.jdbc.transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.apache.commons.dbcp2.BasicDataSource
import java.sql.Connection
import java.sql.SQLException
import kotlin.coroutines.CoroutineContext

/**
 * Singleton to hold the primary database connection as a [DataSource][javax.sql.DataSource] and anything else linked to
 * that DataSource.
 */
object Database {
    private val dataSource = BasicDataSource().apply {
        driverClassName = System.getenv("CLASSNAME")
        url = System.getenv("URL")
        username = System.getenv("DBUSER")
        password = System.getenv("PASSWORD")
        defaultAutoCommit = true
    }

    private val logger = KotlinLogging.logger {}
    /** Coroutine Scope used to run coroutines on the IO [Dispatchers] with the [dataSource] as a context element */
    val scope = CoroutineScope(Dispatchers.IO + CoroutineDataSource(dataSource))

    /**
     * Runs the provided lambda with the current coroutineContext's connection or a new connection from the [dataSource]
     * if a connection is not currently open in the context
     */
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

    /**
     * Runs the provided lambda in a blocking manner with a connection from the [dataSource]. Frees the connection when
     * done
     */
    fun <T> runWithConnectionBlocking(func: (Connection) -> T): T {
        return dataSource.connection.use(func)
    }

    /**
     * Runs the provided lambda in a non-blocking context with a connection for the current context of the database
     * [scope]. Frees the connection when done
     */
    suspend inline fun <reified T> runWithConnection(crossinline func: (Connection) -> T): T {
        return withContext(scope.coroutineContext) {
            useConnection {
                func(it)
            }
        }
    }

    /**
     * Runs the provided suspending lambda in a non-blocking context with a connection for the current context of the
     * database [scope]. Frees the connection when done
     */
    suspend inline fun <reified T> runWithConnectionAsync(crossinline func: suspend (Connection) -> T): T {
        return withContext(scope.coroutineContext) {
            useConnection {
                func(it)
            }
        }
    }

    /**
     * Runs the provided suspending lambda in a non-blocking context within a transaction for the current context of the
     * database [scope]. Commits the transaction when done
     */
    suspend inline fun <T> useTransaction(
        crossinline func: suspend CoroutineScope.(Connection) -> T
    ): T {
        return withContext(scope.coroutineContext) {
            transaction {
                func(coroutineContext.connection)
            }
        }
    }

    /** Checks to see if the current [CoroutineContext] has an open connection to use */
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
