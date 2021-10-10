package orm.tables

import org.ktorm.schema.text
import orm.entities.Prov

object Provs: DbTable<Prov>("provs") {

    val provCode = text("prov_code").primaryKey().bindTo { it.provCode }
    val name = text("name").bindTo { it.name }
    val countryCode = text("country_code").bindTo { it.countryCode }
    val countryName = text("country_name").bindTo { it.countryName }

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
}