package database.enums

/**
 * Enum type found in DB denoting run type of task
 * CREATE TYPE public.task_run_type AS ENUM
 *  ('User', 'System');
 */
enum class TaskRunType {
    User,
    System,
}
