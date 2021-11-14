package database.tables

/**
 * Table used to store the province/state codes that can be used to define prov/state level [DataSources]
 */
object Provs: DbTable("provs"), DefaultData {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.provs
        (
            country_code text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(country_code)),
            prov_code text PRIMARY KEY COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(prov_code)),
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)),
            country_name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(country_name))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName = "provs.csv"
}