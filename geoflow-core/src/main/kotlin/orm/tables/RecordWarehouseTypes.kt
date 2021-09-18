package orm.tables

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.text
import orm.entities.RecordWarehouseType

object RecordWarehouseTypes: Table<RecordWarehouseType>("record_warehouse_types") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = text("name").bindTo { it.name }
    val description = text("description").bindTo { it.description }

    val createStatement = """
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

    val createSequence = """
        CREATE SEQUENCE public.record_warehouse_types_id_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 2147483647
            CACHE 1;
    """.trimIndent()
}