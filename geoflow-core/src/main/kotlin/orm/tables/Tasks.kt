package orm.tables

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.text
import orm.entities.Task

object Tasks: Table<Task>("tasks") {
    val taskId = long("task_id").primaryKey().bindTo { it.taskId }
    val name = text("name").bindTo { it.name }
    val description = text("description").bindTo { it.description }
    val parentTaskId = long("parent_task_id").bindTo { it.parentTaskId }
    val state = text("state").bindTo { it.state }
    val parentTaskOrder = int("parent_task_order").bindTo { it.parentTaskOrder }
    val taskRunType = text("task_run_type").bindTo { it.taskRunType }
    val taskClassName = text("task_class_name").bindTo { it.taskClassName }

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.tasks
        (
            name text COLLATE pg_catalog."default" NOT NULL,
            description text COLLATE pg_catalog."default" NOT NULL,
            parent_task_id bigint,
            state text COLLATE pg_catalog."default" NOT NULL,
            parent_task_order integer,
            task_run_type text COLLATE pg_catalog."default" NOT NULL,
            task_id bigint NOT NULL DEFAULT nextval('tasks_task_id_seq'::regclass),
            CONSTRAINT tasks_pkey PRIMARY KEY (task_id)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    val createSequence = """
        CREATE SEQUENCE public.tasks_task_id_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()
}