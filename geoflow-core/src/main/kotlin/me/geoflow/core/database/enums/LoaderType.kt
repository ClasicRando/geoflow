package me.geoflow.core.database.enums

/**
 * Enum type found in DB denoting the type of loader required for given file. Each enum value has [extensions]
 * associated with the value to easy finding of the appropriate loader per filename
 * CREATE TYPE public.loader_type AS ENUM
 * ('Excel', 'Flat', 'DBF', 'MDB');
 */
sealed class LoaderType(
    loaderType: String,
    /** File extensions associated with each LoaderType value */
    vararg val extensions: String,
) : PgEnum("loader_type", loaderType) {
    /** Excel file type loader. Covers 'xlsx' and 'xls' files */
    object Excel : LoaderType(excel, "xlsx", "xls")
    /** Flat file type loader. Covers 'csv', 'tsv' and 'txt' files */
    object Flat : LoaderType(flat, "csv", "tsv", "txt")
    /** DBF file type loader. Covers 'dbf' files */
    object DBF : LoaderType(dbf, "dbf")
    /** Microsoft database file type loader. Covers 'mdb' and 'accdb' files */
    object MDB : LoaderType(mdb, "mdb", "accdb")

    companion object {
        private val types by lazy {
            listOf(
                Excel,
                Flat,
                DBF,
                MDB,
            )
        }
        private const val excel = "Excel"
        private const val flat = "Flat"
        private const val dbf = "DBF"
        private const val mdb = "MDB"
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
            return types.firstOrNull { extension in it.extensions }
                ?: throw IllegalArgumentException("Extension must does not match a supported file type")
        }
    }
}
