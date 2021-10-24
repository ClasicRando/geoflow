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
        fun getLoaderType(fileName: String): LoaderType {
            val fileExtension = "(?<=\\.)[^.]+\$".toRegex().find(fileName)?.value
                ?: throw IllegalArgumentException("file extension could not be found")
            return getLoaderTypeFromExtension(fileExtension)
        }

        private fun getLoaderTypeFromExtension(extension: String): LoaderType {
            return values().first { extension in it.extensions }
        }
    }
}