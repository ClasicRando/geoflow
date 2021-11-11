package database.tables

/**
 * Table used to store the metadata of the columns found in the files/tables from [SourceTables]
 *
 * When a source file is analyzed the column metadata is inserted into this table to alert a user if the file has
 * changed in the columns provided or the character length of data.
 */
object SourceTableColumns: DbTable("source_table_columns"), SequentialPrimaryKey {

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
            column_index integer NOT NULL,
            CONSTRAINT source_table_columns_pkey PRIMARY KEY (stc_oid),
            CONSTRAINT column_name_in_table UNIQUE (st_oid, name)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}