package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.runUpdate
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.functions.UserHasRun.requireUserRun
import me.geoflow.core.database.tables.records.PlottingFieldBody
import java.sql.Connection

/**
 * Table has yet to be finalized and should not be used until then
 */
object PlottingFields : DbTable("plotting_fields") {

    @Suppress("MaxLineLength")
    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.plotting_fields
        (
            run_id bigint NOT NULL REFERENCES public.pipeline_runs (run_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            file_id text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(file_id)),
            name text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(name)),
            address_line1 text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(address_line1)),
            address_line2 text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(address_line2)),
            city text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(city)),
            alternate_cities text[] COLLATE pg_catalog."default" CHECK (check_array_not_blank_or_empty(alternate_cities)),
            mail_code text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(mail_code)),
            latitude text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(latitude)),
            longitude text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(longitude)),
            prov text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(prov)),
            clean_address text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(clean_address)),
            clean_city text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(clean_city)),
            CONSTRAINT plotting_fields_pkey PRIMARY KEY (run_id, file_id)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    /** Returns a list of API response objects representing DB records for the provided [runId] */
    fun getRecords(connection: Connection, runId: Long): List<PlottingFieldBody> {
        return connection.submitQuery(sql = "SELECT * FROM $tableName WHERE run_id = ?", runId)
    }

    /**
     * Inserts a new record into the table with the record information passed to the API in the request body.
     *
     * @throws NoRecordAffected
     */
    fun createRecord(connection: Connection, userOid: Long, record: PlottingFieldBody) {
        requireUserRun(connection, userOid, record.runId)
        connection.runReturningFirstOrNull<String>(
            sql = """
                INSERT INTO $tableName(run_id,file_id,name,address_line1,address_line2,city,mail_code,latitude,
                                       longitude,prov,clean_address,clean_city)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent(),
            record.runId,
            record.fileId,
            record.name,
            record.addressLine1,
            record.addressLine2,
            record.city,
            record.mailCode,
            record.latitude,
            record.longitude,
            record.prov,
            record.cleanCity,
            record.cleanCity,
        ) ?: throw NoRecordAffected(
            tableName,
            "No record inserted for runId = ${record.runId} & file_id = ${record.fileId}",
        )
    }

    /**
     * Updates the record specified by the record information passed to the API in the request body.
     *
     * @throws NoRecordAffected
     */
    fun updateRecord(connection: Connection, userOid: Long, record: PlottingFieldBody): PlottingFieldBody {
        requireUserRun(connection, userOid, record.runId)
        return connection.runReturningFirstOrNull(
            sql = """
                UPDATE $tableName
                SET    name = ?,
                       address_line1 = ?,
                       address_line2 = ?,
                       city = ?,
                       mail_code = ?,
                       latitude = ?,
                       longitude = ?,
                       prov = ?,
                       clean_address = ?,
                       clean_city = ?
                WHERE  run_id = ?
                AND    file_id = ?
                RETURNING run_id,file_id,name,address_line1,address_line2,city,mail_code,latitude,
                          longitude,prov,clean_address,clean_city
            """.trimIndent(),
            record.name,
            record.addressLine1,
            record.addressLine2,
            record.city,
            record.mailCode,
            record.latitude,
            record.longitude,
            record.prov,
            record.cleanCity,
            record.cleanCity,
            record.runId,
            record.fileId,
        ) ?: throw NoRecordAffected(
            tableName,
            "No record updated for runId = ${record.runId} & file_id = ${record.fileId}",
        )
    }

    /**
     * Deletes the record specified by the runId and fileId values passed as path parameters in the API
     *
     * @throws NoRecordAffected
     */
    fun deleteRecord(connection: Connection, userOid: Long, runId: Long, fileId: String) {
        requireUserRun(connection, userOid, runId)
        val countAffected = connection.runUpdate(
            sql = """
                DELETE FROM $tableName
                WHERE  run_id = ?
                AND    file_id = ?
            """.trimIndent(),
            runId,
            fileId,
        )
        if (countAffected != 1) {
            throw NoRecordAffected(
                tableName,
                "No record deleted for runId = $runId & file_id = $fileId",
            )
        }
    }

}
