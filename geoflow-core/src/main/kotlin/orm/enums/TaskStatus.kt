package orm.enums

/**
 * Enum type found in DB denoting the task status
 * CREATE TYPE public.task_status AS ENUM
 * ('Waiting', 'Scheduled', 'Running', 'Complete');
 */
enum class TaskStatus {
    Waiting,
    Scheduled,
    Running,
    Complete
}