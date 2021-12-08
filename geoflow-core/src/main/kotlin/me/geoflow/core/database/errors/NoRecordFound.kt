package me.geoflow.core.database.errors

/** Exception thrown when a query returns an empty [ResultSet][java.sql.ResultSet] */
class NoRecordFound(tableName: String, message: String)
    : Throwable("No records found in $tableName. $message")
