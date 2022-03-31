package me.geoflow.core.database.enums

/**
 * Enum type found in DB denoting the type of merge of source files
 * CREATE TYPE public.merge_type AS ENUM
 * ('None', 'Exclusive', 'Intersect');
 */
sealed class MergeType(mergeType: String) : PgEnum("merge_type", mergeType) {
    /** Merge type where no files are merged with each other */
    object None : MergeType(none)
    /** Merge type where 2 or more datasets are loaded as non-overlapping sets */
    object Exclusive : MergeType(exclusive)
    /** Merge type where 2 or more datasets are loaded as overlapping sets with common records */
    object Intersect : MergeType(intersect)

    companion object {
        /** */
        fun fromString(mergeType: String): MergeType {
            return when(mergeType) {
                none -> None
                exclusive -> Exclusive
                intersect -> Intersect
                else -> error("Could not find a merge_type for '$mergeType'")
            }
        }
        private const val none = "None"
        private const val exclusive = "Exclusive"
        private const val intersect = "Intersect"
    }

}
