package orm.tables

import org.ktorm.schema.int
import org.ktorm.schema.text
import orm.entities.RecordWarehouseType

/**
 * Table used to store variations of data warehousing options used for moving staging data into production
 *
 * Each record dictates how matching to and merging staging data into production data should be treated
 */
object RecordWarehouseTypes: DbTable<RecordWarehouseType>("record_warehouse_types"), SequentialPrimaryKey {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = text("name").bindTo { it.name }
    val description = text("description").bindTo { it.description }

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.record_warehouse_types
        (
            name text COLLATE pg_catalog."default" NOT NULL,
            description text COLLATE pg_catalog."default" NOT NULL,
            id integer NOT NULL DEFAULT nextval('record_warehouse_types_id_seq'::regclass),
            CONSTRAINT record_warehouse_types_pkey PRIMARY KEY (id)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
