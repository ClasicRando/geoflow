package me.geoflow.core.database.tables

import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.RecordWarehouseType
import java.sql.Connection

/**
 * Table used to store variations of data warehousing options used for moving staging data into production
 *
 * Each record dictates how matching to and merging staging data into production data should be treated
 */
object RecordWarehouseTypes : DbTable("record_warehouse_types"), DefaultData {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.record_warehouse_types
        (
			id integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)),
            description text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(description))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName: String = "record_warehouse_types.csv"

    /** Returns a list of all record warehouse types currently available */
    fun getRecords(connection: Connection): List<RecordWarehouseType> {
        return connection.submitQuery(sql = "SELECT * FROM $tableName")
    }

}
