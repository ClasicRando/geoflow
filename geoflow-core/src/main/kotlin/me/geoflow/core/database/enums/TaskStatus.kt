package me.geoflow.core.database.enums

/**
 * Enum type found in DB denoting the task status
 * CREATE TYPE public.task_status AS ENUM
 * ('Waiting', 'Scheduled', 'Running', 'Complete', 'Failed');
 */
sealed class TaskStatus(status: String) : PgEnum("task_status", status) {

    /** State of the task is waiting to be run as part of the pipeline */
    object Waiting : TaskStatus(waiting)
    /** State of the task is scheduled in the KJob repository so a worker can pick it up for running */
    object Scheduled: TaskStatus(scheduled)
    /** State of the task is currently running in a worker */
    object Running : TaskStatus(running)
    /** State of the task is completed by a worker */
    object Complete : TaskStatus(complete)
    /** State of the task is failed due to an exception during task execution */
    object Failed : TaskStatus(failed)
    /** */
    object RuleBroken : TaskStatus(ruleBroken)

    companion object {
        /** */
        inline fun <reified T: TaskStatus> String.isTaskStatus(): Boolean {
            return fromString(this) is T
        }
        /** */
        fun fromString(status: String): TaskStatus {
            return when(status) {
                waiting -> Waiting
                scheduled -> Scheduled
                running -> Running
                complete -> Complete
                failed -> Failed
                ruleBroken -> RuleBroken
                else -> error("Could not find a task_status for '$status'")
            }
        }
        private const val waiting = "Waiting"
        private const val scheduled = "Scheduled"
        private const val running = "Running"
        private const val complete = "Complete"
        private const val failed = "Failed"
        private const val ruleBroken = "Rule Broken"
    }

}
