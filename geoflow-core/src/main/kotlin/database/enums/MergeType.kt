package database.enums

import org.postgresql.util.PGobject

/**
 * Enum type found in DB denoting the type of merge of source files
 * CREATE TYPE public.merge_type AS ENUM
 * ('None', 'Exclusive', 'Intersect');
 */
enum class MergeType {
    /** Merge type where no files are merged with each other */
    None,
    /** Merge type where 2 or more datasets are loaded as non-overlapping sets */
    Exclusive,
    /** Merge type where 2 or more datasets are loaded as overlapping sets with common records */
    Intersect
    ;

    /** [PGobject] representation of the enum value */
    val pgObject: PGobject = PGobject().apply {
        type = "merge_type"
        value = name
    }
}
