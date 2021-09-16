package orm.tables

import org.ktorm.schema.Table
import org.ktorm.schema.long
import org.ktorm.schema.text
import orm.entities.Pipeline

object Pipelines: Table<Pipeline>("pipelines") {
    val pipelineId = long("pipeline_id").primaryKey().bindTo { it.pipelineId }
    val name = text("name").bindTo { it.name }
    val workflowOperation = text("workflow_operation").references(WorkflowOperations) { it.workflowOperation }

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.pipelines
        (
            pipeline_id bigint NOT NULL DEFAULT nextval('pipelines_pipeline_id_seq'::regclass),
            name text COLLATE pg_catalog."default" NOT NULL,
            workflow_operation text COLLATE pg_catalog."default" NOT NULL,
            CONSTRAINT pipelines_pkey PRIMARY KEY (pipeline_id)
                REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE SET NULL
                NOT VALID
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    val createSequence = """
        CREATE SEQUENCE public.pipelines_pipeline_id_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()
}