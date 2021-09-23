package orm.enums

/**
 * Enum type found in DB denoting the type of merge of source files
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
}