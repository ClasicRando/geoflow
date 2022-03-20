package me.geoflow.core.database.tables

import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.PipelineRelationshipField
import java.sql.Connection

/** */
object PipelineRelationshipFields : DbTable("pipeline_relationship_fields"), ApiExposed {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_relationship_fields
        (
            field_id bigint,
			field_is_generated boolean NOT NULL,
            parent_field_id bigint NOT NULL,
			parent_field_is_generated boolean NOT NULL,
        	st_oid bigint NOT NULL REFERENCES public.source_tables (st_oid) MATCH SIMPLE
        		ON UPDATE CASCADE
        		ON DELETE CASCADE,
			CONSTRAINT relationship_field_unique UNIQUE (field_id,field_is_generated)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "column_name" to mapOf(),
        "field_is_generated" to mapOf("title" to "Is Generated?", "formatter" to "isGenerated"),
        "parent_column_name" to mapOf(),
        "parent_field_is_generated" to mapOf("title" to "Is Generated?", "formatter" to "isGenerated"),
    )

    /** */
    fun getRecords(connection: Connection, stOid: Long): List<PipelineRelationshipField> {
        return connection.submitQuery(
            sql = """
                SELECT t1.field_id, t1.field_is_generated, t1.parent_field_id, t1.parent_field_is_generated, t1.st_oid,
                       COALESCE(t2.name, t3.name), COALESCE(t4.name, t5.name)
                FROM   $tableName t1
                LEFT JOIN ${SourceTableColumns.tableName} t2
                ON     t1.field_id = t2.stc_oid AND NOT t1.field_is_generated
                LEFT JOIN ${GeneratedTableColumns.tableName} t3
                ON     t1.field_id = t3.gtc_oid AND t1.field_is_generated 
                LEFT JOIN ${SourceTableColumns.tableName} t4
                ON     t1.parent_field_id = t4.stc_oid AND NOT t1.parent_field_is_generated
                LEFT JOIN ${GeneratedTableColumns.tableName} t5
                ON     t1.parent_field_id = t5.gtc_oid AND t1.parent_field_is_generated 
                WHERE  t1.st_oid = ?
            """.trimIndent(),
            stOid,
        )
    }

}
