package orm.tables

import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.text
import orm.entities.SourceTableColumn

/**
 * Table used to store the metadata of the columns found in the files/tables from [SourceTables]
 *
 * When a source file is analyzed the column metadata is inserted into this table to alert a user if the file has
 * changed in the columns provided or the character length of data.
 */
object SourceTableColumns: DbTable<SourceTableColumn>("source_table_columns"), SequentialPrimaryKey {
    val stcOid = long("stc_oid").primaryKey().bindTo { it.stcOid }
    val name = text("name").bindTo { it.name }
    val type = text("type").bindTo { it.type }
    val maxLength = int("max_length").bindTo { it.maxLength }
    val minLength = int("min_length").bindTo { it.minLength }
    val label = text("label").bindTo { it.label }
    val stOid = long("st_oid").bindTo { it.stOid }

    val createSequence = """
        CREATE SEQUENCE public.source_table_columns_stc_oid_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()
    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.source_table_columns
        (
            st_oid bigint NOT NULL,
            name text COLLATE pg_catalog."default" NOT NULL,
            type text COLLATE pg_catalog."default" NOT NULL,
            max_length integer NOT NULL,
            min_length integer NOT NULL,
            label text COLLATE pg_catalog."default" NOT NULL,
            stc_oid bigint NOT NULL DEFAULT nextval('source_table_columns_stc_oid_seq'::regclass),
            CONSTRAINT source_table_columns_pkey PRIMARY KEY (stc_oid),
            CONSTRAINT column_name_in_table UNIQUE (st_oid, name)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}