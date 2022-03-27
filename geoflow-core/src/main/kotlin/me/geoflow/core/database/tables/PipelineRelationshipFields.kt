package me.geoflow.core.database.tables

import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.PipelineRelationshipField
import java.sql.Connection

/** */
object PipelineRelationshipFields : DbTable("pipeline_relationship_fields"), ApiExposed, Triggers {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_relationship_fields
        (
            field_id bigint,
			field_is_generated boolean NOT NULL,
            parent_field_id bigint NOT NULL,
			parent_field_is_generated boolean NOT NULL,
        	st_oid bigint NOT NULL REFERENCES public.source_tables (st_oid) MATCH SIMPLE
        		ON UPDATE CASCADE
        		ON DELETE CASCADE,
			CONSTRAINT relationship_field_unique UNIQUE (field_id,field_is_generated)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    @Suppress("MaxLineLength")
    override val triggers: List<Trigger> = listOf(
        Trigger(
            trigger = """
                CREATE TRIGGER trg_check_relationship_fields
                    BEFORE INSERT OR UPDATE 
                    ON public.pipeline_relationship_fields
                    FOR EACH ROW
                    EXECUTE FUNCTION public.check_relationship_fields();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.check_relationship_fields()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                DECLARE
                    check_field boolean;
                BEGIN
                    IF NEW.field_is_generated THEN
                        SELECT EXISTS(SELECT 1
                                      FROM   generated_table_columns
                                      WHERE  gtc_oid = NEW.field_id
                                      AND    st_oid = NEW.st_oid)
                        INTO   check_field;
                        ASSERT check_field, format('Could not find the field_id (%s) as a generated column for st_oid = %s', NEW.field_id, NEW.st_oid);
                    ELSE
                        SELECT EXISTS(SELECT 1
                                      FROM   source_table_columns
                                      WHERE  stc_oid = NEW.field_id
                                      AND    st_oid = NEW.st_oid)
                        INTO   check_field;
                        ASSERT check_field, format('Could not find the field_id (%s) as a source column for st_oid = %s', NEW.field_id, NEW.st_oid);
                    END IF;
                    IF NEW.parent_field_is_generated THEN
                        SELECT EXISTS(SELECT 1
                                      FROM   generated_table_columns gtc
                                      JOIN   pipeline_relationships pr
                                      ON     gtc.st_oid = pr.parent_st_oid
                                      WHERE  gtc.gtc_oid = NEW.parent_field_id
                                      AND    pr.st_oid = NEW.st_oid)
                        INTO   check_field;
                        ASSERT check_field, format('Could not find the field_id (%s) as a generated column for st_oid''s (%s) parent', NEW.parent_field_id, NEW.st_oid);
                    ELSE
                        SELECT EXISTS(SELECT 1
                                      FROM   source_table_columns stc
                                      JOIN   pipeline_relationships pr
                                      ON     stc.st_oid = pr.parent_st_oid
                                      WHERE  stc.stc_oid = NEW.parent_field_id
                                      AND    pr.st_oid = NEW.st_oid)
                        INTO   check_field;
                        ASSERT check_field, format('Could not find the field_id (%s) as a source column for st_oid''s (%s) parent', NEW.parent_field_id, NEW.st_oid);
                    END IF;
                    IF TG_OP = 'INSERT' THEN
                        SELECT EXISTS(SELECT 1
                                      FROM   pipeline_relationship_fields
                                      WHERE  st_oid = NEW.st_oid
                                      AND    parent_field_id = NEW.parent_field_id
                                      AND    parent_field_is_generated = NEW.parent_field_is_generated)
                        INTO   check_field;
                        ASSERT NOT check_field, format('Found another usage of the parent_field (id = %s, is_generated? %s) within this st_oid (%s)', NEW.parent_field_id, NEW.parent_field_is_generated, NEW.st_oid);
                    END IF;
                    RETURN NEW;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent(),
        ),
        Trigger(
            trigger = """
                CREATE TRIGGER trg_check_relationship_fields_insert_statement
                    AFTER INSERT
                    ON public.pipeline_relationship_fields
                    REFERENCING NEW TABLE AS new_table
                    FOR EACH STATEMENT
                    EXECUTE FUNCTION public.check_relationship_fields_statement();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.check_relationship_fields_statement()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                DECLARE
                    v_st_oid bigint;
                    check_relationship boolean;
                    count_relationships int;
                    check_st_oid_count int;
                    check_field_id bigint;
                    check_field_generated boolean;
                BEGIN
                    SELECT st_oid
                    INTO   v_st_oid
                    FROM   new_table
                    LIMIT 1;
                    SELECT EXISTS (SELECT 1 FROM pipeline_relationships WHERE st_oid = v_st_oid)
                    INTO   check_relationship;
                    ASSERT check_relationship, format('Could not find a relationship for st_oid = %s', v_st_oid);
                    
                    SELECT COUNT(DISTINCT st_oid)
                    INTO   check_st_oid_count
                    FROM   new_table;
                    ASSERT check_st_oid_count = 1, 'You cannot update or insert for multiple st_oid';
                    
                    SELECT field_id, field_is_generated
                    INTO   check_field_id, check_field_generated
                    FROM   new_table
                    GROUP BY field_id, field_is_generated
                    HAVING COUNT(0) > 1
                    LIMIT 1;
                    ASSERT check_field_id IS NULL, format('Found a field that is referenced twice. id = %s, is generated? %s', check_field_id, check_field_generated);
                    
                    SELECT parent_field_id, parent_field_is_generated
                    INTO   check_field_id, check_field_generated
                    FROM   new_table
                    GROUP BY parent_field_id, parent_field_is_generated
                    HAVING COUNT(0) > 1
                    LIMIT 1;
                    ASSERT check_field_id IS NULL, format('Found a parent field that is referenced twice. id = %s, is generated? %s', check_field_id, check_field_generated);
                    RETURN NULL;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent(),
        ),
        Trigger(
            trigger = """
                CREATE TRIGGER trg_check_relationship_fields_update_statement
                    AFTER UPDATE 
                    ON public.pipeline_relationship_fields
                    REFERENCING NEW TABLE AS new_table
                    FOR EACH STATEMENT
                    EXECUTE FUNCTION public.check_relationship_fields_statement();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.check_relationship_fields_statement()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                DECLARE
                    v_st_oid bigint;
                    check_relationship boolean;
                    count_relationships int;
                    check_st_oid_count int;
                    check_field_id bigint;
                    check_field_generated boolean;
                BEGIN
                    SELECT st_oid
                    INTO   v_st_oid
                    FROM   new_table
                    LIMIT 1;
                    SELECT EXISTS (SELECT 1 FROM pipeline_relationships WHERE st_oid = v_st_oid)
                    INTO   check_relationship;
                    ASSERT check_relationship, format('Could not find a relationship for st_oid = %s', v_st_oid);
                    
                    SELECT COUNT(DISTINCT st_oid)
                    INTO   check_st_oid_count
                    FROM   new_table;
                    ASSERT check_st_oid_count = 1, 'You cannot update or insert for multiple st_oid';
                    
                    SELECT field_id, field_is_generated
                    INTO   check_field_id, check_field_generated
                    FROM   new_table
                    GROUP BY field_id, field_is_generated
                    HAVING COUNT(0) > 1
                    LIMIT 1;
                    ASSERT check_field_id IS NULL, format('Found a field that is referenced twice. id = %s, is generated? %s', check_field_id, check_field_generated);
                    
                    SELECT parent_field_id, parent_field_is_generated
                    INTO   check_field_id, check_field_generated
                    FROM   new_table
                    GROUP BY parent_field_id, parent_field_is_generated
                    HAVING COUNT(0) > 1
                    LIMIT 1;
                    ASSERT check_field_id IS NULL, format('Found a parent field that is referenced twice. id = %s, is generated? %s', check_field_id, check_field_generated);
                    RETURN NULL;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent(),
        ),
    )

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "column_name" to mapOf(),
        "field_is_generated" to mapOf("title" to "Is Generated?", "formatter" to "isGenerated"),
        "parent_column_name" to mapOf(),
        "parent_field_is_generated" to mapOf("title" to "Is Generated?", "formatter" to "isGenerated"),
    )

    /** */
    fun getRecords(connection: Connection, stOid: Long): List<PipelineRelationshipField> {
        return connection.submitQuery(
            sql = """
                SELECT t1.field_id, t1.field_is_generated, t1.parent_field_id, t1.parent_field_is_generated, t1.st_oid,
                       COALESCE(t2.name, t3.name), COALESCE(t4.name, t5.name)
                FROM   $tableName t1
                LEFT JOIN ${SourceTableColumns.tableName} t2
                ON     t1.field_id = t2.stc_oid AND NOT t1.field_is_generated
                LEFT JOIN ${GeneratedTableColumns.tableName} t3
                ON     t1.field_id = t3.gtc_oid AND t1.field_is_generated 
                LEFT JOIN ${SourceTableColumns.tableName} t4
                ON     t1.parent_field_id = t4.stc_oid AND NOT t1.parent_field_is_generated
                LEFT JOIN ${GeneratedTableColumns.tableName} t5
                ON     t1.parent_field_id = t5.gtc_oid AND t1.parent_field_is_generated 
                WHERE  t1.st_oid = ?
            """.trimIndent(),
            stOid,
        )
    }

}
