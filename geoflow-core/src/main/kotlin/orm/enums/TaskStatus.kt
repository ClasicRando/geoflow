package orm.enums

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
}