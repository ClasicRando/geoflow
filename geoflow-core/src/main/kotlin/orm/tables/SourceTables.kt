package orm.tables

import org.ktorm.schema.*
import orm.entities.SourceTable
import orm.enums.FileCollectType
import orm.enums.LoaderType

object SourceTables: Table<SourceTable>("source_tables") {

    val stOid = long("st_oid").primaryKey().bindTo { it.stOid }
    val runId = long("run_id").bindTo { it.runId }
    val sourceTableName = text("table_name").bindTo { it.tableName }
    val fileName = text("file_name").bindTo { it.fileName }
    val analyze = boolean("analyze").bindTo { it.analyze }
    val load = boolean("load").bindTo { it.load }
    val loaderType = enum<LoaderType>("loader_type").bindTo { it.loaderType }
    val qualified = boolean("qualified").bindTo { it.qualified }
    val encoding = text("encoding").bindTo { it.encoding }
    val subTable = text("sub_table").bindTo { it.subTable }
    val recordCount = int("record_count").bindTo { it.recordCount }
    val fileId = text("file_id").bindTo { it.fileId }
    val url = text("url").bindTo { it.url }
    val comments = text("comments").bindTo { it.comments }
    val collectType = enum<FileCollectType>("collect_type").bindTo { it.collectType }
    val delimiter = varchar("delimiter").bindTo { it.delimiter }

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
            qualified boolean NOT NULL,
            encoding text COLLATE pg_catalog."default" NOT NULL,
            sub_table text COLLATE pg_catalog."default",
            record_count integer NOT NULL DEFAULT 0,
            file_id text COLLATE pg_catalog."default" NOT NULL,
            url text COLLATE pg_catalog."default",
            comments text COLLATE pg_catalog."default",
            st_oid bigint NOT NULL DEFAULT nextval('source_tables_st_oid_seq'::regclass),
            collect_type file_collect_type NOT NULL,
            loader_type loader_type NOT NULL,
            delimiter character(1) COLLATE pg_catalog."default",
            CONSTRAINT source_tables_pkey PRIMARY KEY (st_oid)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}