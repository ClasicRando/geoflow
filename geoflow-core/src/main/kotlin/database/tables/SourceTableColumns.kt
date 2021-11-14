package database.tables

/**
 * Table used to store the metadata of the columns found in the files/tables from [SourceTables]
 *
 * When a source file is analyzed the column metadata is inserted into this table to alert a user if the file has
 * changed in the columns provided or the character length of data.
 */
object SourceTableColumns: DbTable("source_table_columns") {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.source_table_columns
        (
            stc_oid bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            st_oid bigint NOT NULL REFERENCES public.source_tables (st_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)),
            type text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(type)),
            max_length integer NOT NULL,
            min_length integer NOT NULL,
            label text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(label)),
            column_index integer NOT NULL,
            CONSTRAINT column_name_table UNIQUE (st_oid, name)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}