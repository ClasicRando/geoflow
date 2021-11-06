package database

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import java.io.File

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
    val database = Database.connect(dataSource)
}