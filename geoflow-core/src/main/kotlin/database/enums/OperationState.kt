package database.enums

import org.postgresql.util.PGobject

/**
 * Enum type found in DB denoting the current state of a run
 * CREATE TYPE public.operation_state AS ENUM
 * ('Ready', 'Active');
 */
enum class OperationState : PostgresEnum {
    /** Operation state of the [PipelineRun][database.tables.PipelineRuns] is ready to be picked up */
    Ready,
    /** Operation state of the [PipelineRun][database.tables.PipelineRuns] is actively held by a user */
    Active,
    ;

    override val pgObject: PGobject = PGobject().apply {
        type = "operation_state"
        value = name
    }
}
