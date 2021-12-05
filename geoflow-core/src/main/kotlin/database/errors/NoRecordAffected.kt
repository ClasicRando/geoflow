package database.errors

/** Exception thrown when a query does not affect any records */
class NoRecordAffected(tableName: String, message: String)
    : Throwable("DML statement did not impact any record in $tableName. $message")
