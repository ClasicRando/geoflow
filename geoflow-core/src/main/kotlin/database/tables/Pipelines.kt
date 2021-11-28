package database.tables

/**
 * Table used to store the top level information of generic data pipelines
 *
 * Named for easier access and categorized by workflow operation the pipeline is associated with
 */
object Pipelines : DbTable("pipelines"), DefaultData {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipelines
        (
            pipeline_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)) UNIQUE,
            workflow_operation text COLLATE pg_catalog."default" NOT NULL
                REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName: String = "pipelines.csv"
}
