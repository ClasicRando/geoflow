package orm.tables

import org.ktorm.schema.*
import orm.entities.PlottingMethod

object PlottingMethods: DbTable<PlottingMethod>("plotting_methods") {
    val dsId = long("ds_id").bindTo { it.dsId }
    val order = short("order").bindTo { it.order }
    val methodType = int("method_type").references(PlottingMethodTypes) { it.methodType }
    val fileId = text("file_id").bindTo { it.fileId }

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.plotting_methods
        (
            ds_id bigint NOT NULL,
            "order" smallint NOT NULL,
            method_type integer NOT NULL,
            file_id text COLLATE pg_catalog."default" NOT NULL
        )
        WITH (
            OIDS = TRUE
        );
    """.trimIndent()
}