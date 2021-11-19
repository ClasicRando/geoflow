package database.tables

object FileTypes : DbTable("file_types"), DefaultData {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.file_types
        (
            file_extension text COLLATE pg_catalog."default" NOT NULL,
            loader_type loader_type NOT NULL,
            CONSTRAINT file_types_pkey PRIMARY KEY (file_extension)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName: String = "file_types.csv"
}
