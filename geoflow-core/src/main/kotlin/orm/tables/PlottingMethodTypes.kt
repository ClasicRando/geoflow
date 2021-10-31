package orm.tables

import org.ktorm.schema.int
import org.ktorm.schema.text
import orm.entities.PlottingMethodType

/**
 * Table has yet to be finalized and should not be used until then
 */
object PlottingMethodTypes: DbTable<PlottingMethodType>("plotting_method_types"), SequentialPrimaryKey {
    val methodId = int("method_id").primaryKey().bindTo { it.methodId }
    val name = text("name").bindTo { it.name }

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.plotting_method_types
        (
            name text COLLATE pg_catalog."default" NOT NULL,
            method_id integer NOT NULL DEFAULT nextval('plotting_method_types_method_id_seq'::regclass),
            CONSTRAINT plotting_method_types_pkey PRIMARY KEY (method_id)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
