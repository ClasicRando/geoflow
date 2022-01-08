package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.extensions.getSqlArray
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.runUpdate
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.functions.UserHasRun.requireUserRun
import me.geoflow.core.database.tables.records.PlottingFieldBody
import me.geoflow.core.database.tables.records.PlottingFieldsRequest
import java.sql.Connection

/**
 * Table used to store the plotting fields of source tables that will be used to store locations. Links to a pipeline
 * run using run_id and a source table using file_id
 */
object PlottingFields : DbTable("plotting_fields"), ApiExposed, Triggers {

    @Suppress("MaxLineLength")
    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.plotting_fields
        (
            st_oid bigint NOT NULL REFERENCES public.source_tables (st_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
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
            CONSTRAINT plotting_fields_pkey PRIMARY KEY (st_oid)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "table_name" to mapOf(),
        "name" to mapOf("title" to "Company Name"),
        "address_line1" to mapOf("title" to "Address Line 1"),
        "address_line2" to mapOf("title" to "Address Line 2"),
        "city" to mapOf(),
        "alternate_cities" to mapOf("formatter" to "alternateCitiesFormatter"),
        "mail_code" to mapOf(),
        "latitude" to mapOf(),
        "longitude" to mapOf(),
        "prov" to mapOf("title" to "Prov/State"),
        "clean_address" to mapOf("title" to "Cleaned Address"),
        "clean_city" to mapOf("title" to "Cleaned City"),
        "action" to mapOf("formatter" to "plottingFieldsActions")
    )

    @Suppress("MaxLineLength")
    override val triggers: List<Trigger> = listOf(
        Trigger(
            trigger = """
                CREATE TRIGGER edit_plotting_fields
                    BEFORE INSERT OR UPDATE 
                    ON public.plotting_fields
                    FOR EACH ROW
                    EXECUTE FUNCTION public.plotting_fields_action();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.plotting_fields_action()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                declare
                    source_table_cols text[];
                    col_names text[];
                    duplicates boolean;
                begin
                    select array_agg(t1.name) into source_table_cols
                    from   source_table_columns t1
                    join   source_tables t2
                    on     t1.st_oid = t2.st_oid
                    where  t2.st_oid = new.st_oid;
                    
                    select array_agg(col_name) into col_names
                    from  (select unnest(array[new.name, new.address_line1, new.address_line2, new.city,
                                               new.mailing_code, new.prov, new.latitude, new.longitude]) col_name
                           union all
                           select unnest(new.alternate_citites)
                           ) t1
                    where  col_name is not null;
    
                    if col_names = '{}' then
                        delete from plotting_fields
                        where  st_oid = new.st_oid;
                        return null;
                    end if;
                    
                    select string_agg(col_name,',') into bad_fields
                    from  (select unnest(col_names) col_name
                           except
                           select unnest(source_table_cols) col_name
                           ) t1;
                    
                    if bad_fields != '' then
                        raise exception 'Plotting fields cannot be verified since not all field names specified can be found (%)', bad_fields;
                    end if;
                    
                    select exists(select 1
                				  from  (select unnest(col_names) col_name) t1
                				  group by col_name
                				  having count(0) > 1)
                	into duplicates;
                    
                    if duplicates then
                        raise exception 'Plotting fields cannot be verified since a field name is duplicated';
                    end if;
                    return new;
                end;
                ${'$'}BODY${'$'};
            """.trimIndent()
        )
    )

    /** Returns a list of API response objects representing DB records for the provided [runId] */
    fun getRecords(connection: Connection, runId: Long): List<PlottingFieldBody> {
        return connection.submitQuery(
            sql = """
                SELECT t1.st_oid,t2.table_name,t1.name,t1.address_line1,t1.address_line2,t1.city,t1.alternate_cities,
                       t1.mail_code,t1.latitude,t1.longitude,t1.prov,t1.clean_address,t1.clean_city
                FROM   $tableName t1
                JOIN   ${SourceTables.tableName} t2
                ON     t1.st_oid = t2.st_oid
                WHERE  t2.run_id = ?
            """.trimIndent(),
            runId)
    }

    /** Returns a list of API response objects representing DB records for the provided [stOid] */
    fun getSourceTableRecords(connection: Connection, stOid: Long): List<PlottingFieldBody> {
        return connection.submitQuery(
            sql = """
                SELECT t1.st_oid,t2.table_name,t1.name,t1.address_line1,t1.address_line2,t1.city,t1.alternate_cities,
                       t1.mail_code,t1.latitude,t1.longitude,t1.prov,t1.clean_address,t1.clean_city
                FROM   $tableName t1
                JOIN   ${SourceTables.tableName} t2
                ON     t1.st_oid = t2.st_oid
                WHERE  t1.st_oid = ?
            """.trimIndent(),
            stOid,
        )
    }

    /**
     * Inserts a new record into the table with the record information passed to the API in the request body. If there
     * is already a record for the request's st_oid, an update is performed with the request content.
     *
     * @throws NoRecordAffected
     */
    fun setRecord(connection: Connection, userOid: Long, record: PlottingFieldsRequest) {
        val runId = SourceTables.getRunId(connection, record.stOid)
        requireUserRun(connection, userOid, runId)
        connection.runReturningFirstOrNull<Long>(
            sql = """
                INSERT INTO $tableName(st_oid,name,address_line1,address_line2,city,alternate_cities,mail_code,
                                       latitude,longitude,prov,clean_address,clean_city)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT (st_oid) DO UPDATE SET name = ?,
                                                   address_line1 = ?,
                                                   address_line2 = ?,
                                                   city = ?,
                                                   alternate_cities = ?,
                                                   mail_code = ?,
                                                   latitude = ?,
                                                   longitude = ?,
                                                   prov = ?,
                                                   clean_address = ?,
                                                   clean_city = ?
                RETURNING st_oid
            """.trimIndent(),
            record.stOid,
            record.name?.takeIf { it.isNotBlank() },
            record.addressLine1?.takeIf { it.isNotBlank() },
            record.addressLine2?.takeIf { it.isNotBlank() },
            record.city?.takeIf { it.isNotBlank() },
            record.alternateCities?.takeIf { it.isNotEmpty() }?.getSqlArray(connection),
            record.mailCode?.takeIf { it.isNotBlank() },
            record.latitude?.takeIf { it.isNotBlank() },
            record.longitude?.takeIf { it.isNotBlank() },
            record.prov?.takeIf { it.isNotBlank() },
            record.cleanCity?.takeIf { it.isNotBlank() },
            record.cleanCity?.takeIf { it.isNotBlank() },
            record.name?.takeIf { it.isNotBlank() },
            record.addressLine1?.takeIf { it.isNotBlank() },
            record.addressLine2?.takeIf { it.isNotBlank() },
            record.city?.takeIf { it.isNotBlank() },
            record.alternateCities?.takeIf { it.isNotEmpty() }?.getSqlArray(connection),
            record.mailCode?.takeIf { it.isNotBlank() },
            record.latitude?.takeIf { it.isNotBlank() },
            record.longitude?.takeIf { it.isNotBlank() },
            record.prov?.takeIf { it.isNotBlank() },
            record.cleanCity?.takeIf { it.isNotBlank() },
            record.cleanCity?.takeIf { it.isNotBlank() },
        ) ?: throw NoRecordAffected(
            tableName,
            "No record affected for st_oid = ${record.stOid}",
        )
    }

    /**
     * Deletes the record specified by the runId and fileId values passed as path parameters in the API
     *
     * @throws NoRecordAffected
     */
    fun deleteRecord(connection: Connection, userOid: Long, stOid: Long) {
        val runId = SourceTables.getRunId(connection, stOid)
        requireUserRun(connection, userOid, runId)
        val countAffected = connection.runUpdate(
            sql = """
                DELETE FROM $tableName
                WHERE  st_oid = ?
            """.trimIndent(),
            stOid,
        )
        if (countAffected != 1) {
            throw NoRecordAffected(tableName,"No record deleted for st_oid = $stOid")
        }
    }

}
