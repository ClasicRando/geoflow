package orm.tables

import org.ktorm.schema.*
import orm.entities.PipelineRun

object PipelineRuns: Table<PipelineRun>("pipeline_runs") {
    val runId = long("run_id").primaryKey().bindTo { it.runId }
    val dsId = long("ds_id").references(DataSources) { it.dataSource }
    val recordDate = date("record_date").bindTo { it.recordDate }
    val workflowOperation = text("workflow_operation").bindTo { it.workflowOperation }
    val operationState = text("operation_state").bindTo { it.operationState }
    val collectionUser = long("collection_user_oid").references(InternalUsers) { it.collectionUser }
    val loadUser = long("load_user_oid").references(InternalUsers) { it.loadUser }
    val checkUser = long("check_user_oid").references(InternalUsers) { it.checkUser }
    val qaUser = long("qa_user_oid").references(InternalUsers) { it.qaUser }

    val createSequence = """
        CREATE SEQUENCE public.pipeline_runs_run_id_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.pipeline_runs
        (
            ds_id bigint NOT NULL,
            record_date date NOT NULL,
            load_user_oid bigint,
            check_user_oid bigint,
            qa_user_oid bigint,
            collection_user_oid bigint,
            workflow_operation text COLLATE pg_catalog."default" NOT NULL,
            operation_state text COLLATE pg_catalog."default" NOT NULL,
            run_id bigint NOT NULL DEFAULT nextval('pipeline_runs_run_id_seq'::regclass),
            CONSTRAINT pipeline_runs_pkey PRIMARY KEY (run_id),
            CONSTRAINT ds_id FOREIGN KEY (ds_id)
                REFERENCES public.data_sources (ds_id) MATCH SIMPLE
                ON UPDATE NO ACTION
                ON DELETE CASCADE
                NOT VALID
        )
        WITH (
            OIDS = FALSE
        )
    """.trimIndent()
}