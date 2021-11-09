package orm.enums

import org.postgresql.util.PGobject

/**
 * Enum type found in DB denoting the current state of a run
 * CREATE TYPE public.operation_state AS ENUM
 * ('Ready', 'Active');
 */
enum class OperationState {
    Ready,
    Active,
    ;

    val pgObject = PGobject().apply {
        type = "operation_state"
        value = name
    }
}