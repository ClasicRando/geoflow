package me.geoflow.core.database.enums

/**
 * Enum type found in DB denoting the current state of a run
 * CREATE TYPE public.operation_state AS ENUM
 * ('Ready', 'Active');
 */
sealed class OperationState(operationState: String) : PgEnum("operation_state", operationState) {
    /** Operation state of the [PipelineRun][me.geoflow.core.database.tables.PipelineRuns] is ready to be picked up */
    object Ready : OperationState(ready)
    /** Operation state of the [PipelineRun][me.geoflow.core.database.tables.PipelineRuns] is actively held by a user */
    object Active : OperationState(active)

    companion object {
        /** */
        fun fromString(operationState: String): OperationState {
            return when(operationState) {
                ready -> Ready
                active -> Active
                else -> error("Could not find a operation_state for '$operationState'")
            }
        }
        private const val ready = "Ready"
        private const val active = "Active"
    }

}
