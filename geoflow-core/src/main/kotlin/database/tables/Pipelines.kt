package database.tables

/**
 * Table used to store the top level information of generic data pipelines
 *
 * Named for easier access and categorized by workflow operation the pipeline is associated with
 */
object Pipelines: DbTable("pipelines"), SequentialPrimaryKey {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.pipelines
        (
            pipeline_id bigint NOT NULL DEFAULT nextval('pipelines_pipeline_id_seq'::regclass),
            name text COLLATE pg_catalog."default" NOT NULL,
            workflow_operation text COLLATE pg_catalog."default" NOT NULL,
            CONSTRAINT pipelines_pkey PRIMARY KEY (pipeline_id),
            CONSTRAINT workflow_operations FOREIGN KEY (workflow_operation)
                REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE SET NULL
                NOT VALID
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
