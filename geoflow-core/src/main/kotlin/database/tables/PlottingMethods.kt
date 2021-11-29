package database.tables

/**
 * Table has yet to be finalized and should not be used until then
 */
object PlottingMethods : DbTable("plotting_methods") {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.plotting_methods
        (
            ds_id bigint NOT NULL,
            "order" smallint NOT NULL,
            method_type integer NOT NULL,
            file_id text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(file_id)),
            CONSTRAINT plotting_methods_pkey PRIMARY KEY (ds_id, "order")
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
