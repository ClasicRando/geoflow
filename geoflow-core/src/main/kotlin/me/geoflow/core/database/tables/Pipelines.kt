package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.errors.NoRecordFound
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.Pipeline
import java.sql.Connection

/**
 * Table used to store the top level information of generic data pipelines
 *
 * Named for easier access and categorized by workflow operation the pipeline is associated with
 */
object Pipelines : DbTable("pipelines"), DefaultData {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipelines
        (
            pipeline_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)) UNIQUE,
            workflow_operation text COLLATE pg_catalog."default" NOT NULL
                REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName: String = "pipelines.csv"

    /** Returns a single pipeline record for the given [pipelineId] */
    fun getRecord(connection: Connection, pipelineId: Long): Pipeline {
        return connection.queryFirstOrNull(sql = "SELECT * FROM $tableName WHERE pipeline_id = ?", pipelineId)
            ?: throw NoRecordFound(tableName, "Could not find a record for pipeline_id = $pipelineId")
    }

    /** Returns a list of all pipelines */
    fun getRecords(connection: Connection): List<Pipeline> {
        return connection.submitQuery(sql = "SELECT * FROM $tableName")
    }

    /** Updates pipeline name */
    fun updateName(connection: Connection, userId: Long, pipeline: Pipeline) {
        requireNotNull(pipeline.pipelineId) { "Cannot update a record with a null pipeline ID" }
        InternalUsers.requireRole(connection, userId, "pipeline_edit")
        connection.runReturningFirstOrNull<Long>(
            sql = """
                UPDATE $tableName
                SET    name = ?
                WHERE  pipeline_id = ?
                RETURNING pipeline_id
            """.trimIndent(),
            pipeline.name,
            pipeline.pipelineId,
        ) ?: throw NoRecordAffected(tableName, "No record updated for pipeline_id = ${pipeline.pipelineId}")
    }

    /**
     * Attempts to create a record using the [pipeline] object provided
     *
     * @throws IllegalStateException when pipeline_id of the [pipeline] object is not null
     * @throws me.geoflow.core.database.errors.IllegalUserAction when the user does not have the privilege required
     * @throws java.sql.SQLException when the connection throws an exception
     */
    fun createRecord(connection: Connection, userId: Long, pipeline: Pipeline): Long {
        require(pipeline.pipelineId == null) { "Request body object cannot provide a pipeline_id" }
        InternalUsers.requireRole(connection, userId, "pipeline_edit")
        return connection.runReturningFirstOrNull(
            sql = """
                INSERT INTO $tableName(name,workflow_operation)
                VALUES(?,?)
                RETURNING pipeline_id
            """.trimIndent(),
            pipeline.name,
            pipeline.workflowOperation,
        ) ?: throw NoRecordAffected(tableName, "No record inserted into pipeline table")
    }

}
