package orm.tables

import org.ktorm.schema.enum
import org.ktorm.schema.text
import orm.entities.FileType
import orm.enums.LoaderType

object FileTypes: DbTable<FileType>("file_types"), DefaultData {

    val fileExtension = text("file_extension").primaryKey().bindTo { it.fileExtension }
    val loaderType = enum<LoaderType>("loader_type").bindTo { it.loaderType }

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