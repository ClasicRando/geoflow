package orm.enums

import org.postgresql.util.PGobject

/**
 * Enum type found in DB denoting the type of collection to obtain the source file
 * CREATE TYPE public.file_collect_type AS ENUM
 * ('Download', 'FOI', 'Email', 'Scrape', 'Collect', 'REST');
 */
enum class FileCollectType {
    Download,
    FOI,
    Email,
    Scrape,
    Collect,
    REST,
    ;

    val pgObject = PGobject().apply {
        type = "file_collect_type"
        value = name
    }
}