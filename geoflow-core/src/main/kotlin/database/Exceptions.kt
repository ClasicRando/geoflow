package database

/** */
class NoRecordFound(tableName: String, message: String)
    : Throwable("No records found in $tableName. $message")

/** */
class NoRecordAffected(tableName: String, message: String)
    : Throwable("DML statement did not impact any record in $tableName. $message")


