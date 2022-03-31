package me.geoflow.core.database.enums

import org.postgresql.util.PGobject

/** */
abstract class PgEnum(enumType: String, enumValue: String) : PGobject() {
    init {
        type = enumType
        value = enumValue
    }
}
