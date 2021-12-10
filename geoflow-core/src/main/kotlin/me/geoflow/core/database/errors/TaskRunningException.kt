package me.geoflow.core.database.errors

/**
 * Exception thrown when an operation on [PipelineRunTasks][me.geoflow.core.database.tables.PipelineRunTasks] tries to
 * change a record when the run has an active task
 */
class TaskRunningException(prTaskId: Long) : Throwable("Cannot perform operation while task = $prTaskId is running")
