package me.geoflow.core.database.tables

/**
 * Table used to store variations of data warehousing options used for moving staging data into production
 *
 * Each record dictates how matching to and merging staging data into production data should be treated
 */
object RecordWarehouseTypes : DbTable("record_warehouse_types") {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.record_warehouse_types
        (
			id integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)),
            description text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(description))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
