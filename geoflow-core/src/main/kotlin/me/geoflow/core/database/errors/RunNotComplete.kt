package me.geoflow.core.database.errors

/**
 * Exception thrown when a user attempts to move a run to the next state but not all the tasks in the current state are
 * complete
 */
class RunNotComplete(runId: Long)
    : Throwable("Pipeline Run (id = $runId) cannot move forward since not all tasks are complete")
