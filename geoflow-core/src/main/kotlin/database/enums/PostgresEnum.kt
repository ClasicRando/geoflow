package database.enums

import org.postgresql.util.PGobject

/** Marks enum class as having a PGobject property */
interface PostgresEnum {

    /** [PGobject] representation of the enum value */
    val pgObject: PGobject

}
