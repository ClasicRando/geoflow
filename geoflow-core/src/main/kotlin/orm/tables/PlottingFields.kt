package orm.tables

import org.ktorm.schema.long
import org.ktorm.schema.text
import org.ktorm.support.postgresql.textArray
import orm.entities.PlottingField

object PlottingFields: DbTable<PlottingField>("plotting_fields") {
    val name = text("name").bindTo { it.name }
    val addressLine1 = text("address_line1").bindTo { it.addressLine1 }
    val addressLine2 = text("address_line2").bindTo { it.addressLine2 }
    val city = text("city").bindTo { it.city }
    val alternativeCities = textArray("alternative_cities").bindTo { it.alternativeCities }
    val dsId = long("ds_id").bindTo { it.dsId }
    val mailCode = text("mail_code").bindTo { it.mailCode }
    val latitude = text("latitude").bindTo { it.latitude }
    val longitude = text("longitude").bindTo { it.longitude }
    val prov = text("prov").bindTo { it.prov }
    val fileId = text("file_id").bindTo { it.fileId }
    val cleanAddress = text("clean_address").bindTo { it.cleanAddress }
    val cleanCity = text("clean_city").bindTo { it.cleanCity }

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.plotting_fields
        (
            name text COLLATE pg_catalog."default",
            address_line1 text COLLATE pg_catalog."default",
            address_line2 text COLLATE pg_catalog."default",
            city text COLLATE pg_catalog."default",
            alternate_cities text[] COLLATE pg_catalog."default",
            ds_id bigint NOT NULL,
            mail_code text COLLATE pg_catalog."default",
            latitude text COLLATE pg_catalog."default",
            longitude text COLLATE pg_catalog."default",
            prov text COLLATE pg_catalog."default",
            file_id text COLLATE pg_catalog."default" NOT NULL,
            clean_address text COLLATE pg_catalog."default",
            clean_city text COLLATE pg_catalog."default"
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}