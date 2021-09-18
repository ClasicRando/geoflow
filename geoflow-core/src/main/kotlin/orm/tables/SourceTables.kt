package orm.tables

import org.ktorm.schema.*
import orm.entities.SourceTable

object SourceTables: Table<SourceTable>("source_tables") {

    val stOid = long("st_oid").primaryKey().bindTo { it.stOid }
    val runId = long("run_id").bindTo { it.runId }
    val sourceTableName = text("table_name").bindTo { it.tableName }
    val fileName = text("file_name").bindTo { it.fileName }
    val analyze = boolean("analyze").bindTo { it.analyze }
    val load = boolean("load").bindTo { it.load }
    val fileType = text("file_type").bindTo { it.fileType }
    val qualified = boolean("qualified").bindTo { it.qualified }
    val encoding = text("encoding").bindTo { it.encoding }
    val subTable = text("sub_table").bindTo { it.subTable }
    val recordCount = int("record_count").bindTo { it.recordCount }
    val fileId = text("file_id").bindTo { it.fileId }
    val url = text("url").bindTo { it.url }
    val comments = text("comments").bindTo { it.comments }

    val createSequence = """
        CREATE SEQUENCE public.source_tables_st_oid_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()
    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.source_tables
        (
            run_id bigint NOT NULL,
            table_name text COLLATE pg_catalog."default" NOT NULL,
            file_name text COLLATE pg_catalog."default" NOT NULL,
            "analyze" boolean NOT NULL,
            load boolean NOT NULL,
            file_type text COLLATE pg_catalog."default" NOT NULL,
            qualified boolean NOT NULL,
            encoding text COLLATE pg_catalog."default" NOT NULL,
            sub_table text COLLATE pg_catalog."default",
            record_count integer NOT NULL,
            file_id text COLLATE pg_catalog."default" NOT NULL,
            url text COLLATE pg_catalog."default" NOT NULL,
            comments text COLLATE pg_catalog."default",
            st_oid bigint NOT NULL DEFAULT nextval('source_tables_st_oid_seq'::regclass),
            CONSTRAINT source_tables_pkey PRIMARY KEY (st_oid)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}