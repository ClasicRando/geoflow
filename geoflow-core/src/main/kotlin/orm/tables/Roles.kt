package orm.tables

import org.ktorm.schema.Table
import org.ktorm.schema.text
import orm.entities.Role

object Roles: Table<Role>("roles") {
    val name = text("name").primaryKey().bindTo { it.name }
    val description = text("description").bindTo { it.description }

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.roles
        (
            name text COLLATE pg_catalog."default" NOT NULL,
            description text COLLATE pg_catalog."default" NOT NULL,
            CONSTRAINT roles_pkey PRIMARY KEY (name)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}