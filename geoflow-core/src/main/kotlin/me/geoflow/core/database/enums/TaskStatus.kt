package me.geoflow.core.database.enums

import org.postgresql.util.PGobject

/**
 * Enum type found in DB denoting the task status
 * CREATE TYPE public.task_status AS ENUM
 * ('Waiting', 'Scheduled', 'Running', 'Complete', 'Failed');
 */
enum class TaskStatus : PostgresEnum {
    /** State of the task is waiting to be run as part of the pipeline */
    Waiting,
    /** State of the task is scheduled in the KJob repository so a worker can pick it up for running */
    Scheduled,
    /** State of the task is currently running in a worker */
    Running,
    /** State of the task is completed by a worker */
    Complete,
    /** State of the task is failed due to an exception during task execution */
    Failed,
    ;

    override val pgObject: PGobject = PGobject().apply {
        type = "task_status"
        value = name
    }
}
