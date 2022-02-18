package me.geoflow.core.database.composites

import org.postgresql.util.PGobject

/** */
abstract class Composite(
    /** */
    val typeName: String,
) {

    /** */
    abstract val createStatement: String
    /** */
    abstract val compositeValue: String

    /** */
    val getPgObject: PGobject get() {
        return PGobject().apply {
            type = typeName
            value = compositeValue
        }
    }

}
