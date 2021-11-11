package database.tables

/**
 * Table used to store the province/state codes that can be used to define prov/state level [DataSources]
 */
object Provs: DbTable("provs"), DefaultData {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.provs
        (
            country_code text COLLATE pg_catalog."default" NOT NULL,
            prov_code text COLLATE pg_catalog."default" NOT NULL,
            name text COLLATE pg_catalog."default" NOT NULL,
            country_name text COLLATE pg_catalog."default" NOT NULL,
            CONSTRAINT prov_pkey PRIMARY KEY (prov_code)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName = "provs.csv"
}