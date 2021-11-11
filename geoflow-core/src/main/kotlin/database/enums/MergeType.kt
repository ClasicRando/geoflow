package database.enums

/**
 * Enum type found in DB denoting the type of merge of source files
 * CREATE TYPE public.merge_type AS ENUM
 * ('None', 'Exclusive', 'Intersect');
 */
enum class MergeType {
    None,
    Exclusive,
    Intersect
}