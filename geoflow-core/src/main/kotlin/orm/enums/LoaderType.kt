package orm.enums

/**
 * Enum type found in DB denoting the type of loader required for given file. Each enum value has extensions associated
 * with the value to easy finding of the appropriate loader per filename
 * CREATE TYPE public.loader_type AS ENUM
 * ('Excel', 'Flat', 'DBF', 'MDB');
 */
enum class LoaderType(val extensions: List<String>) {
    Excel(listOf("xlsx", "xls")),
    Flat(listOf("csv", "tsv", "txt")),
    DBF(listOf("dbf")),
    MDB(listOf("mdb", "accdb")),
    ;

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