package me.geoflow.web.utils

/** Utility class for a tuple of 4 values */
data class Quad<A, B, C, D>(
    /** first value */
    val first: A,
    /** second value */
    val second: B,
    /** third value */
    val third: C,
    /** fourth value */
    val fourth: D,
)
