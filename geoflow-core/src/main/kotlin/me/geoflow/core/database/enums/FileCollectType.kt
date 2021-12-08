package me.geoflow.core.database.enums

import org.postgresql.util.PGobject

/**
 * Enum type found in DB denoting the type of collection to obtain the source file
 * CREATE TYPE public.file_collect_type AS ENUM
 * ('Download', 'FOI', 'Email', 'Scrape', 'Collect', 'REST');
 */
enum class FileCollectType: PostgresEnum {
    /** Download collection type */
    Download,
    /** FOI response collection type */
    FOI,
    /** Department Email response collection type */
    Email,
    /** Webpage/site scrape collection type */
    Scrape,
    /** General collection type of non-specific collection methods */
    Collect,
    /** ArcGIS REST service collection type */
    REST,
    ;

    override val pgObject: PGobject = PGobject().apply {
        type = "file_collect_type"
        value = name
    }
}
