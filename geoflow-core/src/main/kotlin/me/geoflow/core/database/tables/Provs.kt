package me.geoflow.core.database.tables

import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.Prov
import java.sql.Connection

/**
 * Table used to store the province/state codes that can be used to define prov/state level [DataSources]
 */
object Provs : DbTable("provs"), DefaultData {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.provs
        (
            country_code text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(country_code)),
            prov_code text PRIMARY KEY COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(prov_code)),
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)),
            country_name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(country_name))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName: String = "provs.csv"

    /** Returns the full list of [Prov] records */
    fun getRecords(connection: Connection): List<Prov> {
        return connection.submitQuery(sql = "SELECT prov_code, name, country_code, country_name FROM $tableName")
    }

}
