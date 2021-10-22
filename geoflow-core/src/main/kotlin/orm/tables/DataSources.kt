package orm.tables

import org.ktorm.schema.*
import orm.entities.DataSource

/**
 * Table used to store sources of data. Core backing of how data is grouped and moves through the system from collection
 * to reporting.
 *
 * The records contain data about how the source is moved through the system, treated during reporting and other meta
 * details about the source
 *
 * Future Changes
 * --------------
 * - implement API requirements for users to perform CRUD operations on this table
 */
object DataSources: DbTable<DataSource>("data_sources") {
    val dsId = long("ds_id").primaryKey().bindTo { it.dsId }
    val code = text("code").bindTo { it.code }
    val country = text("country").bindTo { it.country }
    val prov = text("prov").bindTo { it.prov }
    val description = text("description").bindTo { it.description }
    val filesLocation = text("files_location").bindTo { it.filesLocation }
    val provLevel = boolean("prov_level").bindTo { it.provLevel }
    val comments = text("comments").bindTo { it.comments }
    val assignedUser = long("assigned_user").references(InternalUsers) { it.assignedUser }
    val created = timestamp("created").bindTo { it.created }
    val createdBy = long("created_by").references(InternalUsers) { it.createdBy }
    val lastUpdated = timestamp("last_updated").bindTo { it.lastUpdated }
    val updatedBy = long("updated_by").references(InternalUsers) { it.updatedBy }
    val searchRadius = double("search_radius").bindTo { it.searchRadius }
    val recordWarehouseType = int("record_warehouse_type").references(RecordWarehouseTypes) { it.recordWarehouseType }
    val reportingType = text("reporting_type").bindTo { it.reportingType }
    val collectionPipeline = long("collection_pipeline").bindTo { it.collectionPipeline }
    val loadPipeline = long("load_pipeline").bindTo { it.loadPipeline }
    val checkPipeline = long("check_pipeline").bindTo { it.checkPipeline }
    val qaPipeline = long("qa_pipeline").bindTo { it.qaPipeline }

    val createSequence = """
        CREATE SEQUENCE public.data_sources_ds_id_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()
    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.data_sources
        (
            ds_id bigint NOT NULL DEFAULT nextval('data_sources_ds_id_seq'::regclass),
            code text COLLATE pg_catalog."default" NOT NULL,
            country text COLLATE pg_catalog."default" NOT NULL,
            prov text COLLATE pg_catalog."default",
            description text COLLATE pg_catalog."default" NOT NULL,
            files_location text COLLATE pg_catalog."default" NOT NULL,
            prov_level boolean NOT NULL,
            comments text COLLATE pg_catalog."default",
            assigned_user bigint NOT NULL,
            created_by bigint NOT NULL,
            last_updated timestamp without time zone,
            updated_by bigint,
            search_radius double precision NOT NULL,
            record_warehouse_type integer NOT NULL,
            reporting_type text COLLATE pg_catalog."default" NOT NULL,
            created timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
            collection_pipeline bigint NOT NULL,
            load_pipeline bigint,
            check_pipeline bigint,
            qa_pipeline bigint,
            CONSTRAINT data_sources_pkey PRIMARY KEY (ds_id)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}