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
object DataSources : DbTable("data_sources") {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.data_sources
        (
            ds_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            code text COLLATE pg_catalog."default" NOT NULL,
            prov text COLLATE pg_catalog."default" NOT NULL REFERENCES public.provs (prov_code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
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
            last_updated timestamp without time zone,
            updated_by bigint REFERENCES public.internal_users (user_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            search_radius double precision NOT NULL,
            record_warehouse_type integer NOT NULL REFERENCES public.record_warehouse_types (id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            reporting_type text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(reporting_type)),
            created timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
            collection_pipeline bigint NOT NULL REFERENCES public.pipelines (pipeline_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            load_pipeline bigint,
            check_pipeline bigint,
            qa_pipeline bigint
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
