package database

import com.impossibl.postgres.jdbc.PGDataSource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.ktorm.database.Database
import java.io.File

object DatabaseConnection {
    @OptIn(ExperimentalSerializationApi::class)
    private val dataSource = PGDataSource().apply {
        val json = File(System.getProperty("user.dir"), "db_config.json").readText()
        Json.decodeFromString<ConnectionProperties>(json).let { props ->
            serverName = props.serverName
            databaseName = props.dbName
            user = props.username
            password = props.password
        }
    }
    val database = Database.connect(dataSource)
}