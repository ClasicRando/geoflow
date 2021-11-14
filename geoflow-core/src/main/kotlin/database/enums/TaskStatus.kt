package database.enums

import org.postgresql.util.PGobject

/**
 * Enum type found in DB denoting the task status
 * CREATE TYPE public.task_status AS ENUM
 * ('Waiting', 'Scheduled', 'Running', 'Complete', 'Failed');
 */
enum class TaskStatus {
    Waiting,
    Scheduled,
    Running,
    Complete,
    Failed,
    ;

    val pgObject = PGobject().apply {
        type = "task_status"
        value = name
    }
}