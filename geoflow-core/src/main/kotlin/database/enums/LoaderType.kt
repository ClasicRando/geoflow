package database.enums

import org.postgresql.util.PGobject

/**
 * Enum type found in DB denoting the type of loader required for given file. Each enum value has [extensions]
 * associated with the value to easy finding of the appropriate loader per filename
 * CREATE TYPE public.loader_type AS ENUM
 * ('Excel', 'Flat', 'DBF', 'MDB');
 */
enum class LoaderType(
    /** File extensions associated with each LoaderType value */
    val extensions: List<String>,
) : PostgresEnum {
    /** Excel file type loader. Covers 'xlsx' and 'xls' files */
    Excel(listOf("xlsx", "xls")),
    /** Flat file type loader. Covers 'csv', 'tsv' and 'txt' files */
    Flat(listOf("csv", "tsv", "txt")),
    /** DBF file type loader. Covers 'dbf' files */
    DBF(listOf("dbf")),
    /** Microsoft database file type loader. Covers 'mdb' and 'accdb' files */
    MDB(listOf("mdb", "accdb")),
    ;

    override val pgObject: PGobject = PGobject().apply {
        type = "loader_type"
        value = name
    }

    companion object {
        /**
         * Returns a [LoaderType] from the [fileName]. Extracts an extension and calls [getLoaderTypeFromExtension]
         *
         * @throws IllegalArgumentException when the extension cannot be found or the extension is not supported
         */
        fun getLoaderType(fileName: String): LoaderType {
            val fileExtension = "(?<=\\.)[^.]+\$".toRegex().find(fileName)?.value
                ?: throw IllegalArgumentException("File extension could not be found")
            return getLoaderTypeFromExtension(fileExtension)
        }

        /**
         * Returns a [LoaderType] from the [extension].
         *
         * @throws IllegalArgumentException when the extension is not supported
         */
        fun getLoaderTypeFromExtension(extension: String): LoaderType {
            return values().firstOrNull { extension in it.extensions }
                ?: throw IllegalArgumentException("Extension must does not match a supported file type")
        }
    }
}
