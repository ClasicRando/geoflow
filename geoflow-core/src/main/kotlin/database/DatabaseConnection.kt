package database

import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.transaction
import com.github.michaelbull.jdbc.withConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import rowToClass
import java.io.File
import java.sql.Connection

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
    }

    val database by lazy { Database.connect(dataSource) }
    val scope = CoroutineScope(Dispatchers.IO + CoroutineDataSource(dataSource))

    suspend inline fun <reified T> submitQuery(
        sql: String,
        parameters: List<Any?> = listOf(),
    ): List<T> {
        return queryConnection { connection ->
            connection.prepareStatement(sql).apply {
                for (parameter in parameters.withIndex()) {
                    setObject(parameter.index + 1, parameter.value)
                }
            }.use { statement ->
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

    suspend inline fun useTransaction(
        crossinline func: suspend CoroutineScope.(Connection) -> Unit
    ) {
        return withContext(scope.coroutineContext) {
            transaction {
                func(coroutineContext.connection)
            }
        }
    }
}
