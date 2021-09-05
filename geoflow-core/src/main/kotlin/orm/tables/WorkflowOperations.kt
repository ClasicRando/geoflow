package orm.tables

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.text
import orm.entities.WorkflowOperation

object WorkflowOperations: Table<WorkflowOperation>("workflow_operations") {
    val code = text("code").primaryKey().bindTo { it.code }
    val href = text("href").bindTo { it.href }
    val role = text("role").bindTo { it.role }
    val name = text("name").bindTo { it.name }
    val workflowOrder = int("workflow_order").bindTo { it.workflowOrder }

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.workflow_operations
        (
            code text COLLATE pg_catalog."default" NOT NULL,
            href text COLLATE pg_catalog."default" NOT NULL,
            role text COLLATE pg_catalog."default" NOT NULL,
            name text COLLATE pg_catalog."default" NOT NULL,
            workflow_order integer NOT NULL,
            CONSTRAINT workflow_operations_pkey PRIMARY KEY (code)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}