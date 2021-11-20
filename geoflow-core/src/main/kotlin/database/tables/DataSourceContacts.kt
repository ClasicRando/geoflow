package database.tables

/**
 * Table used to store contacts of a DataSource
 *
 * This table should never be used by itself but rather referenced in [DataSources] table
 */
object DataSourceContacts: DbTable("data_source_contacts") {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.data_source_contacts
        (
            contact_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            ds_id bigint NOT NULL REFERENCES public.data_sources (ds_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            name text COLLATE pg_catalog."default" CHECK(check_not_blank_or_empty(name)),
            email text COLLATE pg_catalog."default" CHECK(check_not_blank_or_empty(email)),
            website text COLLATE pg_catalog."default" CHECK(check_not_blank_or_empty(website)),
            type text COLLATE pg_catalog."default" CHECK(check_not_blank_or_empty(type)),
            notes text COLLATE pg_catalog."default" CHECK(check_not_blank_or_empty(notes))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
