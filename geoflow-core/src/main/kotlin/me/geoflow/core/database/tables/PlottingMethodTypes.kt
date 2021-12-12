package me.geoflow.core.database.tables

import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.PlottingMethodType
import java.sql.Connection

/**
 * Table used to store the plotting methods allowed for geocoding a record
 */
object PlottingMethodTypes : DbTable("plotting_method_types") {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.plotting_method_types
        (
            method_id integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)) UNIQUE
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    /** */
    fun getRecords(connection: Connection): List<PlottingMethodType> {
        return connection.submitQuery(sql = "SELECT * FROM $tableName ORDER BY method_id")
    }

}
