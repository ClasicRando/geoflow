package orm.tables

import org.ktorm.schema.long
import org.ktorm.schema.text
import orm.entities.DataSourceContact
import orm.entities.DataSource

/**
 * Table used to store contacts of a [DataSource]
 *
 * This table should never be used by itself but rather referenced in [DataSources] table
 */
object DataSourceContacts: DbTable<DataSourceContact>("data_source_contacts"), SequentialPrimaryKey {
    val contactId = long("contact_id").primaryKey().bindTo { it.contactId }
    val dsId = long("ds_id").bindTo { it.dsId }
    val name = text("name").bindTo { it.name }
    val email = text("email").bindTo { it.email }
    val website = text("website").bindTo { it.website }
    val type = text("type").bindTo { it.type }
    val notes = text("notes").bindTo { it.notes }

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
