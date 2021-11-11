package database.tables

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
object DataSources: DbTable("data_sources"), SequentialPrimaryKey {

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
