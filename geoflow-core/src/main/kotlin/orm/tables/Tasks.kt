package orm.tables

import org.ktorm.schema.*
import orm.entities.Task
import orm.enums.TaskRunType

object Tasks: Table<Task>("tasks") {
    val taskId = long("task_id").primaryKey().bindTo { it.taskId }
    val name = text("name").bindTo { it.name }
    val description = text("description").bindTo { it.description }
    val state = text("state").bindTo { it.state }
    val taskRunType = enum<TaskRunType>("task_run_type").bindTo { it.taskRunType }
    val taskClassName = text("task_class_name").bindTo { it.taskClassName }

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.tasks
        (
            name text COLLATE pg_catalog."default" NOT NULL,
            description text COLLATE pg_catalog."default" NOT NULL,
            state text COLLATE pg_catalog."default" NOT NULL,
            task_id bigint NOT NULL DEFAULT nextval('tasks_task_id_seq'::regclass),
            task_class_name text COLLATE pg_catalog."default" NOT NULL,
            task_run_type task_run_type NOT NULL,
            CONSTRAINT tasks_pkey PRIMARY KEY (task_id),
            CONSTRAINT name_unique UNIQUE (name),
            CONSTRAINT workflow_state_fk FOREIGN KEY (state)
                REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE NO ACTION
                ON DELETE NO ACTION
                NOT VALID
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