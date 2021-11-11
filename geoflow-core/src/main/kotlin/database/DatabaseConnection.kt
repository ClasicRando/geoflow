package database

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.context.dataSource
import com.github.michaelbull.jdbc.transaction
import com.github.michaelbull.jdbc.withConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import org.postgresql.PGConnection
import rowToClass
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

    val logger = KotlinLogging.logger {}
    val database by lazy { Database.connect(dataSource) }
    val scope = CoroutineScope(Dispatchers.IO + CoroutineDataSource(dataSource))

    inline fun CoroutineScope.useConnection(block: CoroutineScope.(Connection) -> Unit) {
        val connection = if (coroutineContext.hasOpenConnection()) {
            coroutineContext.connection
        } else {
            coroutineContext.dataSource.connection
        }
        connection.use {
            block(it)
        }
    }

    suspend inline fun <reified T> submitQuery(
        sql: String,
        parameters: List<Any?> = listOf(),
    ): List<T> {
        return queryConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                for (parameter in parameters.withIndex()) {
                    statement.setObject(parameter.index + 1, parameter.value)
                }
                statement.executeQuery().use { rs ->
                    generateSequence {
                        if (rs.next()) rs.rowToClass<T>() else null
                    }.toList()
                }
            }
        }
    }

    suspend inline fun <T> queryConnection(
        crossinline func: suspend CoroutineScope.(Connection) -> List<T>,
    ): List<T> {
        return withContext(scope.coroutineContext) {
            withConnection {
                func(coroutineContext.connection)
            }
        }
    }

    suspend inline fun <T> queryConnectionSingle(
        crossinline func: suspend CoroutineScope.(Connection) -> T,
    ): T {
        return withContext(scope.coroutineContext) {
            withConnection {
                func(coroutineContext.connection)
            }
        }
    }

    suspend fun execute(
        func: suspend CoroutineScope.(Connection) -> Unit
    ) {
        withContext(scope.coroutineContext) {
            withConnection {
                func(coroutineContext.connection)
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
     * Calls [close][Connection.close] on this [Connection], catching any [SQLException] that was thrown and logging it.
     */
    private fun Connection.closeCatching() {
        try {
            close()
        } catch (ex: SQLException) {
            logger.warn(ex) { "Failed to close database connection cleanly:" }
        }
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
