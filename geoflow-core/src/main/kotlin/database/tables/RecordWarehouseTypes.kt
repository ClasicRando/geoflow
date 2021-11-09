package database.tables

import orm.tables.SequentialPrimaryKey

/**
 * Table used to store variations of data warehousing options used for moving staging data into production
 *
 * Each record dictates how matching to and merging staging data into production data should be treated
 */
object RecordWarehouseTypes: DbTable("record_warehouse_types"), SequentialPrimaryKey {

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