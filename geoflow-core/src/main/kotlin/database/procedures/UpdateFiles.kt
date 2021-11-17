package database.procedures

import java.sql.Connection
import kotlin.reflect.typeOf

/**
 * Stored procedure that performs some standard maintenance of source_tables records.
 *
 * Some examples are:
 * - fixing any record with incorrect loader_type based upon the file extension
 * - setting file_id when null (should be able to delete this later due to NOT NULL constraint)
 * - sets all non-FLAT file records to have a false qualified field
 * - sets sub_table field to null when loader_type is not excel or mdb
 */
object UpdateFiles: SqlProcedure(
    name = "update_files",
    parameterTypes = listOf(
        typeOf<Long>(),
    ),
) {

    /** Call the stored procedure */
    fun call(connection: Connection, pipelineRunTaskId: Long) {
        super.call(connection, pipelineRunTaskId)
    }

    override val code = """
        CREATE OR REPLACE PROCEDURE public.update_files(
        	run_id bigint)
        LANGUAGE 'sql'
        AS         ${'$'}BODY${'$'}
        update source_tables t1
        set    loader_type = t2.loader_type
        from   file_types t2
        where  lower(trim(leading '.' from substring(t1.file_name from '\.[^.]+$'))) = t2.file_extension
        and    t1.run_id = $1;

        with max_file_id as (
        	select max(substring(file_id from '\d+')::INTEGER) max_file_id
        	from   source_tables
        	where  run_id = $1
        ), null_file_ids as (
        	select st_oid, max_file_id + row_number() over (order by st_oid) new_file_id
        	from   source_tables st, max_file_id mf
        	where  run_id = $1
        	and    file_id is null
        )
        update source_tables t1
        set    file_id = 'F'||t2.new_file_id
        from   null_file_ids t2
        where  t1.st_oid = t2.st_oid;
        
        update source_tables
        set    qualified = false
        where  run_id = $1
        and    loader_type != 'Flat';

        update source_tables
        set    sub_table = null
        where  run_id = $1
        and    loader_type not in ('MDB', 'Excel');
        ${'$'}BODY${'$'};
    """.trimIndent()
}