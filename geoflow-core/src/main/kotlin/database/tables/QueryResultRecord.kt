package database.tables

/**
 * Annotation to mark a class as used for converting a [java.sql.ResultSet] to a class
 */
@Target(AnnotationTarget.CLASS)
annotation class QueryResultRecord
