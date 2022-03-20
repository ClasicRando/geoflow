package me.geoflow.core.database.tables

import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.PipelineRelationshipField
import java.sql.Connection

/** */
object PipelineRelationshipFields : DbTable("pipeline_relationship_fields"), ApiExposed {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_relationship_fields
        (
            stc_oid bigint PRIMARY KEY REFERENCES public.source_table_columns (stc_oid) MATCH SIMPLE
        		ON UPDATE CASCADE
        		ON DELETE CASCADE,
            parent_stc_oid bigint NOT NULL REFERENCES public.source_table_columns (stc_oid) MATCH SIMPLE
        		ON UPDATE CASCADE
        		ON DELETE CASCADE,
        	st_oid bigint NOT NULL REFERENCES public.source_tables (st_oid) MATCH SIMPLE
        		ON UPDATE CASCADE
        		ON DELETE CASCADE
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "column_name" to mapOf(),
        "parent_column_name" to mapOf(),
    )

    /** */
    fun getRecords(connection: Connection, stOid: Long): List<PipelineRelationshipField> {
        return connection.submitQuery(
            sql = """
                SELECT t1.stc_oid, t1.parent_stc_oid, t1.st_oid, t2.name, t3.name
                FROM   $tableName t1
                JOIN   ${SourceTableColumns.tableName} t2
                ON     t1.stc_oid = t2.stc_oid
                JOIN   ${SourceTableColumns.tableName} t3
                ON     t1.parent_stc_oid = t3.stc_oid
                WHERE  t1.st_oid = ?
            """.trimIndent(),
            stOid,
        )
    }

}
