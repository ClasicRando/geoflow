package database.enums

import org.postgresql.util.PGobject

/**
 * Enum type found in DB denoting run type of task
 * CREATE TYPE public.task_run_type AS ENUM
 *  ('User', 'System');
 */
enum class TaskRunType : PostgresEnum {
    /** Task run with no action. Task is simply a logical interrupt to warn the user of something */
    User,
    /** Task run with an action. Task runs some underlining code as part of the pipeline */
    System,
    ;

    override val pgObject: PGobject = PGobject().apply {
        type = "task_run_type"
        value = name
    }
}
