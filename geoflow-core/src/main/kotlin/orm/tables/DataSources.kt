package orm.tables

import org.ktorm.schema.*
import orm.entities.DataSource

object DataSources: Table<DataSource>("data_sources") {
    val dsId = long("ds_id").primaryKey().bindTo { it.dsId }
    val code = text("code").primaryKey().bindTo { it.code }
    val country = text("country").primaryKey().bindTo { it.country }
    val prov = text("prov").primaryKey().bindTo { it.prov }
    val description = text("description").primaryKey().bindTo { it.description }
    val filesLocation = text("files_location").primaryKey().bindTo { it.filesLocation }
    val provLevel = boolean("prov_level").primaryKey().bindTo { it.provLevel }

    val createSequence = """
        CREATE SEQUENCE public.data_sources_ds_id_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()
    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.data_sources
        (
            ds_id bigint NOT NULL DEFAULT nextval('data_sources_ds_id_seq'::regclass),
            code text COLLATE pg_catalog."default" NOT NULL,
            country text COLLATE pg_catalog."default" NOT NULL,
            prov text COLLATE pg_catalog."default",
            description text COLLATE pg_catalog."default" NOT NULL,
            files_location text COLLATE pg_catalog."default" NOT NULL,
            prov_level boolean NOT NULL,
            CONSTRAINT data_sources_pkey PRIMARY KEY (ds_id)
        )
        WITH (
            OIDS = FALSE
        )
    """.trimIndent()
}