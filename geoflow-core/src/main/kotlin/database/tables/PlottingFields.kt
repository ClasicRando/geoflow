package database.tables

/**
 * Table has yet to be finalized and should not be used until then
 */
object PlottingFields: DbTable("plotting_fields") {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.plotting_fields
        (
            name text COLLATE pg_catalog."default",
            address_line1 text COLLATE pg_catalog."default",
            address_line2 text COLLATE pg_catalog."default",
            city text COLLATE pg_catalog."default",
            alternate_cities text[] COLLATE pg_catalog."default",
            ds_id bigint NOT NULL,
            mail_code text COLLATE pg_catalog."default",
            latitude text COLLATE pg_catalog."default",
            longitude text COLLATE pg_catalog."default",
            prov text COLLATE pg_catalog."default",
            file_id text COLLATE pg_catalog."default" NOT NULL,
            clean_address text COLLATE pg_catalog."default",
            clean_city text COLLATE pg_catalog."default"
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}