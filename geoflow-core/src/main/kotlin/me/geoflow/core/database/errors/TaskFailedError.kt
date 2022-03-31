package me.geoflow.core.database.errors

/** */
class TaskFailedError(prTaskId: Long) : Throwable("Cannot Run a task (task = $prTaskId) while another task has failed")
