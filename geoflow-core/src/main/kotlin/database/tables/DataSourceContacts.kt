package database.tables

import orm.tables.SequentialPrimaryKey

/**
 * Table used to store contacts of a DataSource
 *
 * This table should never be used by itself but rather referenced in [DataSources] table
 */
object DataSourceContacts: DbTable("data_source_contacts"), SequentialPrimaryKey {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.data_source_contacts
        (
            contact_id bigint NOT NULL DEFAULT nextval('data_source_contacts_contact_id_seq'::regclass),
            ds_id bigint NOT NULL,
            name text COLLATE pg_catalog."default",
            email text COLLATE pg_catalog."default",
            website text COLLATE pg_catalog."default",
            type text COLLATE pg_catalog."default",
            notes text COLLATE pg_catalog."default",
            CONSTRAINT data_source_contacts_pkey PRIMARY KEY (contact_id)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
