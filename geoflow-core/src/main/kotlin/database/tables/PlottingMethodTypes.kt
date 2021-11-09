package database.tables

import orm.tables.SequentialPrimaryKey

/**
 * Table has yet to be finalized and should not be used until then
 */
object PlottingMethodTypes: DbTable("plotting_method_types"), SequentialPrimaryKey {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.plotting_method_types
        (
            name text COLLATE pg_catalog."default" NOT NULL,
            method_id integer NOT NULL DEFAULT nextval('plotting_method_types_method_id_seq'::regclass),
            CONSTRAINT plotting_method_types_pkey PRIMARY KEY (method_id)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}