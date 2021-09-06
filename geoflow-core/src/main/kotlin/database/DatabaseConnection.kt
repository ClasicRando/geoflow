package database

import com.beust.klaxon.Klaxon
import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import java.io.File

object DatabaseConnection {
    private val dataSource = BasicDataSource().apply {
        val json = File(System.getProperty("user.dir"), "db_config.json").readText()
        Klaxon().parse<ConnectionProperties>(json)?.let { props ->
            driverClassName = props.className
            url = props.url
            username = props.username
            password = props.password
        }
    }
    val database = Database.connect(dataSource)
}