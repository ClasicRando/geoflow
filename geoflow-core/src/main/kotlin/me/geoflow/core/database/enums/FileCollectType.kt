package me.geoflow.core.database.enums

/**
 * Enum type found in DB denoting the type of collection to obtain the source file
 * CREATE TYPE public.file_collect_type AS ENUM
 * ('Download', 'FOI', 'Email', 'Scrape', 'Collect', 'REST');
 */
sealed class FileCollectType(collectType: String) : PgEnum("file_collect_type", collectType) {
    /** Download collection type */
    object Download : FileCollectType(download)
    /** FOI response collection type */
    object FOI : FileCollectType(foi)
    /** Department Email response collection type */
    object Email : FileCollectType(email)
    /** Webpage/site scrape collection type */
    object Scrape : FileCollectType(scrape)
    /** General collection type of non-specific collection methods */
    object Collect : FileCollectType(collect)
    /** ArcGIS REST service collection type */
    object REST : FileCollectType(rest)

    companion object {
        /** */
        val values: List<String> by lazy {
            listOf(
                download,
                foi,
                email,
                scrape,
                collect,
                rest,
            )
        }
        /** */
        fun fromString(collectType: String): FileCollectType {
            return when(collectType) {
                download -> Download
                foi -> FOI
                email -> Email
                scrape -> Scrape
                collect -> Collect
                rest -> REST
                else -> error("Could not find a file_collect_type for '$collectType'")
            }
        }
        private const val download = "Download"
        private const val foi = "FOI"
        private const val email = "Email"
        private const val scrape = "Scrape"
        private const val collect = "Collect"
        private const val rest = "REST"
    }

}
