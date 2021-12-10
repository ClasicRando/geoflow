package me.geoflow.core.database.tables

/**
 * Table has yet to be finalized and should not be used until then
 */
object PlottingFields : DbTable("plotting_fields") {

    @Suppress("MaxLineLength")
    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.plotting_fields
        (
            name text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(name)),
            address_line1 text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(address_line1)),
            address_line2 text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(address_line2)),
            city text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(city)),
            alternate_cities text[] COLLATE pg_catalog."default" CHECK (check_array_not_blank_or_empty(alternate_cities)),
            ds_id bigint NOT NULL REFERENCES public.data_sources (ds_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            mail_code text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(mail_code)),
            latitude text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(latitude)),
            longitude text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(longitude)),
            prov text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(prov)),
            file_id text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(file_id)),
            clean_address text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(clean_address)),
            clean_city text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(clean_city)),
            CONSTRAINT plotting_fields_pkey PRIMARY KEY (ds_id, file_id)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
