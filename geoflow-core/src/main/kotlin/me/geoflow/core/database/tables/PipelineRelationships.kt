package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordFound
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.PipelineRelationship
import java.sql.Connection

/** */
object PipelineRelationships : DbTable("pipeline_relationships"), ApiExposed {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_relationships
        (
            st_oid bigint PRIMARY KEY REFERENCES public.source_tables (st_oid) MATCH SIMPLE
        		ON UPDATE CASCADE
        		ON DELETE CASCADE,
            parent_st_oid bigint NOT NULL REFERENCES public.source_tables (st_oid) MATCH SIMPLE
        		ON UPDATE CASCADE
        		ON DELETE CASCADE
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "table_name" to mapOf(),
        "parent_table_name" to mapOf(),
    )

    /** */
    fun getRecords(connection: Connection, runId: Long): List<PipelineRelationship> {
        return connection.submitQuery(
            sql = """
                SELECT t1.st_oid, t1.parent_st_oid, t2.table_name, t3.table_name
                FROM   $tableName t1
                JOIN   ${SourceTables.tableName} t2
                ON     t1.st_oid = t2.st_oid
                JOIN   ${SourceTables.tableName} t3
                ON     t1.parent_st_oid = t3.st_oid
                WHERE  t2.run_id = ?
                AND    t3.run_id = ?
            """.trimIndent(),
            runId,
            runId,
        )
    }

    /** */
    fun getRecord(connection: Connection, stOid: Long): PipelineRelationship {
        return connection.queryFirstOrNull(
            sql = """
                SELECT t1.st_oid, t1.parent_st_oid, t2.table_name, t3.table_name
                FROM   $tableName t1
                JOIN   ${SourceTables.tableName} t2
                ON     t1.st_oid = t2.st_oid
                JOIN   ${SourceTables.tableName} t3
                ON     t1.parent_st_oid = t3.st_oid
                WHERE  t1.st_oid = ?
            """.trimIndent(),
            stOid,
        ) ?: throw NoRecordFound(tableName, "Could not find a relationship for st_oid = $stOid")
    }

}
