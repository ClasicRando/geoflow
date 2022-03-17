package me.geoflow.core.database.tables

/** */
object PipelineLoadingLogic : DbTable("pipeline_loading_logic"), Triggers {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_loading_logic
        (
            st_oid bigint NOT NULL REFERENCES public.source_tables (st_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            parent_st_oid bigint NOT NULL REFERENCES public.source_tables (st_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            linking_field bigint NOT NULL REFERENCES public.source_table_columns (stc_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            parent_linking_field bigint NOT NULL REFERENCES public.source_table_columns (stc_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    @Suppress("MaxLineLength")
    override val triggers: List<Trigger> = listOf(
        Trigger(
            trigger = """
                CREATE TRIGGER trg_check_loading_logic
                    BEFORE INSERT OR UPDATE 
                    ON public.pipeline_loading_logic
                    FOR EACH ROW
                    EXECUTE FUNCTION public.check_loading_logic();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.check_loading_logic()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                DECLARE
                    check_st_oid bigint;
                BEGIN
                    SELECT st_oid
                    INTO   check_st_oid
                    FROM   source_table_columns
                    WHERE  stc_oid = NEW.linking_field;
                    ASSERT check_st_oid = NEW.st_oid, 'Linking field does not exist in the specified source table';
                    SELECT st_oid
                    INTO   check_st_oid
                    FROM   source_table_columns
                    WHERE  stc_oid = NEW.parent_linking_field;
                    ASSERT check_st_oid = NEW.parent_st_oid, 'Parent linking field does not exist in the specified source table';
                    RETURN NEW;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent(),
        )
    )

}
