package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.errors.NoRecordFound
import me.geoflow.core.database.errors.UserMissingRole
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.runUpdate
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.DataSource
import me.geoflow.core.database.tables.records.DataSourceRequest
import java.io.File
import java.sql.Connection

/**
 * Table used to store sources of data. Core backing of how data is grouped and moves through the system from collection
 * to reporting.
 *
 * The records contain data about how the source is moved through the system, treated during reporting and other meta
 * details about the source
 */
object DataSources : DbTable("data_sources"), ApiExposed {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.data_sources
        (
            ds_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            code text COLLATE pg_catalog."default" NOT NULL,
            prov text COLLATE pg_catalog."default" NOT NULL REFERENCES public.provs (prov_code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            country text COLLATE pg_catalog."default" NOT NULL CHECK (country_check(country, prov)),
            description text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(description)),
            files_location text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(files_location)),
            prov_level boolean NOT NULL,
            comments text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(comments)),
            assigned_user bigint NOT NULL REFERENCES public.internal_users (user_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            created_by bigint NOT NULL REFERENCES public.internal_users (user_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            last_updated timestamp with time zone,
            updated_by bigint REFERENCES public.internal_users (user_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            search_radius double precision NOT NULL,
            record_warehouse_type integer NOT NULL REFERENCES public.record_warehouse_types (id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            reporting_type text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(reporting_type)),
            created timestamp with time zone NOT NULL DEFAULT timezone('utc'::text, now()),
            collection_pipeline bigint NOT NULL REFERENCES public.pipelines (pipeline_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            load_pipeline bigint NOT NULL REFERENCES public.pipelines (pipeline_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            check_pipeline bigint NOT NULL REFERENCES public.pipelines (pipeline_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            qa_pipeline bigint NOT NULL REFERENCES public.pipelines (pipeline_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "ds_id" to mapOf("title" to "ID"),
        "code" to mapOf("title" to "Source Code"),
        "prov" to mapOf("title" to "Prov/State"),
        "country" to mapOf("title" to "Country Code"),
        "assigned_user" to mapOf("title" to "Assigned To"),
        "created_by" to mapOf(),
        "created" to mapOf(),
        "updated_by" to mapOf(),
        "last_updated" to mapOf(),
        "search_radius" to mapOf(),
        "record_warehouse_type" to mapOf("title" to "Warehouse Type"),
        "reporting_type" to mapOf(),
        "collection_pipeline" to mapOf(),
        "load_pipeline" to mapOf(),
        "check_pipeline" to mapOf(),
        "qa_pipeline" to mapOf("title" to "QA Pipeline"),
        "actions" to mapOf("formatter" to "dataSourceActionsFormatter"),
    )

    /**
     * Returns a single [DataSource] record for the provided [dsId]
     *
     * @throws NoRecordFound when the dsId cannot be found
     * @throws java.sql.SQLException when the connection throws an exception
     */
    fun getRecord(connection: Connection, dsId: Long): DataSource {
        return connection.queryFirstOrNull(sql = "${DataSource.sql} WHERE ds_id = ?", dsId)
            ?: throw NoRecordFound(tableName, "Could not find a data source record for ds_id = $dsId")
    }

    /**
     * Returns a list of [DataSource] records
     *
     * @throws UserMissingRole when the user does not have the 'ds_create' or 'admin' role
     * @throws java.sql.SQLException when the connection throws an exception
     */
    fun getRecords(connection: Connection, userId: Long): List<DataSource> {
        InternalUsers.requireRole(connection, userId, "collection")
        return connection.submitQuery(sql = DataSource.sql)
    }

    /**
     * Updates the record specified by the dsId in [request] with the contents of the [request].
     *
     * Checks to make sure the prov level and prov field correlate and the user has the role required.
     *
     * @throws IllegalArgumentException when the source is prov level and the prov is null or blank or the source is not
     * prov level and the prov is not null or blank
     * @throws UserMissingRole when the user does not have the 'ds_create' or 'admin' role
     * @throws java.sql.SQLException when the connection throws an exception
     */
    fun updateRecord(connection: Connection, userId: Long, request: DataSourceRequest): DataSource {
        requireNotNull(request.dsId) { "ds_id provided in the request body cannot be null" }
        if (request.provLevel && request.prov.isNullOrBlank()) {
            throw IllegalArgumentException("Specified data source is prov level but the prov field is null or blank")
        }
        if (!request.provLevel && !request.prov.isNullOrBlank()) {
            throw IllegalArgumentException(
                "Specified data source is not prov level but the prov field is not null or blank"
            )
        }
        InternalUsers.requireRole(connection, userId, "collection")
        connection.runUpdate(
            sql = """
                UPDATE $tableName
                SET    code = ?,
                       prov = ?,
                       country = ?,
                       description = ?,
                       files_location = ?,
                       prov_level = ?,
                       comments = ?,
                       assigned_user = ?,
                       search_radius = ?,
                       record_warehouse_type = ?,
                       reporting_type = ?,
                       collection_pipeline = ?,
                       load_pipeline = ?,
                       check_pipeline = ?,
                       qa_pipeline = ?,
                       updated_by = ?,
                       last_updated = timezone('utc'::text, now())
                WHERE  ds_id = ?
            """.trimIndent(),
            request.code,
            request.prov.takeIf { request.provLevel },
            request.country,
            request.description,
            request.filesLocation,
            request.provLevel,
            request.comments?.takeIf { it.isNotBlank() },
            request.assignedUser,
            request.searchRadius,
            request.recordWarehouseType,
            request.reportingType,
            request.collectionPipeline,
            request.loadPipeline,
            request.checkPipeline,
            request.qaPipeline,
            userId,
            request.dsId,
        )
        return getRecord(connection, request.dsId)
    }

    /**
     * Creates a data source record using the [request] data and returns the newly generated ds_id if successful.
     *
     * @throws UserMissingRole the [userId] does not have the 'ds_create' or 'admin' role
     * @throws IllegalArgumentException the provided files_location or search_radius are not valid
     * @throws java.sql.SQLException connection throws exception
     * @throws NoRecordAffected sql statement did not return the newly created ds_id due to an error
     */
    fun createRecord(connection: Connection, userId: Long, request: DataSourceRequest): Long {
        InternalUsers.requireRole(connection, userId, "ds_create")
        require(File(request.filesLocation).exists()) { "File location provided does not exist" }
        require(request.searchRadius > 0) { "Search radius value must be greater than zero" }
        return connection.runReturningFirstOrNull(
            sql = """
                INSERT INTO data_sources(code,prov,country,description,files_location,prov_level,comments,assigned_user,
                                         search_radius,record_warehouse_type,reporting_type,collection_pipeline,
                                         load_pipeline,check_pipeline,qa_pipeline,created_by)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                RETURNING ds_id
            """.trimIndent(),
            request.code,
            request.prov.takeIf { request.provLevel },
            request.country,
            request.description,
            request.filesLocation,
            request.provLevel,
            request.comments?.takeIf { it.isNotBlank() },
            request.assignedUser,
            request.searchRadius,
            request.recordWarehouseType,
            request.reportingType,
            request.collectionPipeline,
            request.loadPipeline,
            request.checkPipeline,
            request.qaPipeline,
            userId,
        ) ?: throw NoRecordAffected(tableName, "No record inserted for ds_id = ${request.dsId}")
    }

}
