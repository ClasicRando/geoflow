package me.geoflow.core.database.tables

/**
 * Table has yet to be finalized and should not be used until then
 */
object PlottingMethodTypes : DbTable("plotting_method_types") {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.plotting_method_types
        (
            method_id integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)) UNIQUE
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
