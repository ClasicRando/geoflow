package orm.enums

/**
 * Enum type found in DB denoting the type of merge of source files
 * CREATE TYPE public.loader_type AS ENUM
 * ('Excel', 'Flat', 'DBF', 'MDB');
 */
enum class LoaderType {
    Excel,
    Flat,
    DBF,
    MDB,
}