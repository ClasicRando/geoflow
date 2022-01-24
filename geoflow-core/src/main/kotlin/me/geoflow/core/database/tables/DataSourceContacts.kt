package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.errors.UserMissingRole
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.runUpdate
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.DataSourceContact
import org.postgresql.util.PGobject
import java.sql.Connection

/**
 * Table used to store contacts of a [DataSource][me.geoflow.core.database.tables.records.DataSource]
 */
object DataSourceContacts: DbTable("data_source_contacts"), ApiExposed {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.data_source_contacts
        (
            contact_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            ds_id bigint NOT NULL REFERENCES public.data_sources (ds_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            name text COLLATE pg_catalog."default" CHECK(check_not_blank_or_empty(name)),
            email email_address COLLATE pg_catalog."default",
            website text COLLATE pg_catalog."default" CHECK(check_not_blank_or_empty(website)),
            type text COLLATE pg_catalog."default" CHECK(check_not_blank_or_empty(type)),
            notes text COLLATE pg_catalog."default" CHECK(check_not_blank_or_empty(notes))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "name" to mapOf(),
        "email" to mapOf(),
        "website" to mapOf(),
        "type" to mapOf(),
        "notes" to mapOf(),
        "actions" to mapOf("formatter" to "dataSourceContactActionsFormatter"),
    )

    /** Returns a list of [DataSourceContact] record for the provided [dsId] */
    fun getRecords(connection: Connection, dsId: Long): List<DataSourceContact> {
        return connection.submitQuery(sql = "SELECT * FROM $tableName WHERE ds_id = ?", dsId)
    }

    /**
     * Updates the record specified by the contactId in the [contact] object. Will not update ds_id field and ds_id
     * value in [contact] must match the existing record.
     *
     * @throws IllegalArgumentException the [contact] contains a null contact_id field
     * @throws UserMissingRole the caller does not have the 'collection' role
     * @throws java.sql.SQLException exception thrown during sql UPDATE
     * @throws NoRecordAffected the update was not successful so the affected count is 0
     */
    fun updateRecord(connection: Connection, userId: Long, contact: DataSourceContact): DataSourceContact {
        requireNotNull(contact.contactId) { "Updating of a data source contact record requires a non-null contact_id" }
        InternalUsers.requireRole(connection, userId, "collection")
        val updateCount = connection.runUpdate(
            sql = """
                UPDATE $tableName
                SET    name = ?,
                       email = ?,
                       website = ?,
                       type = ?,
                       notes = ?
                WHERE  contact_id = ?
                AND    ds_id = ?
            """.trimIndent(),
            contact.name?.takeIf { it.isNotBlank() },
            PGobject().apply {
                type = "email_address"
                value = contact.email?.takeIf { it.isNotBlank() }
            },
            contact.website?.takeIf { it.isNotBlank() },
            contact.type?.takeIf { it.isNotBlank() },
            contact.notes?.takeIf { it.isNotBlank() },
            contact.contactId,
            contact.dsId,
        )
        if (updateCount == 0) {
            throw NoRecordAffected(
                tableName,
                "Update of data source contact was not successful or the contact_id + ds_id does not exist"
            )
        }
        return contact
    }

    /**
     * Creates a new data source contact record for the provided [contact] object. Linked to the ds_id provided within
     * the [contact] record.
     *
     * @throws IllegalArgumentException the [contact] contains a non-null contact_id field
     * @throws UserMissingRole the caller does not have the 'collection' role
     * @throws java.sql.SQLException exception thrown during sql INSERT
     * @throws NoRecordAffected the insert was not successful so the new contact_id was not returned
     */
    fun createRecord(connection: Connection, userId: Long, contact: DataSourceContact): Long {
        require(contact.contactId == null) { "ContactId provided must be null to create a record" }
        InternalUsers.requireRole(connection, userId, "collection")
        return connection.runReturningFirstOrNull(
            sql = """
                INSERT INTO data_source_contacts(name,email,website,type,notes,ds_id)
                VALUES(?,?,?,?,?,?)
                RETURNING contact_id
            """.trimIndent(),
            contact.name?.takeIf { it.isNotBlank() },
            PGobject().apply {
                type = "email_address"
                value = contact.email?.takeIf { it.isNotBlank() }
            },
            contact.website?.takeIf { it.isNotBlank() },
            contact.type?.takeIf { it.isNotBlank() },
            contact.notes?.takeIf { it.isNotBlank() },
            contact.dsId,
        ) ?: throw NoRecordAffected(tableName, "No record inserted into data source contacts")
    }

    /**
     * Deletes the data source contact record for the provided [contactId]
     *
     * @throws UserMissingRole the caller does not have the 'collection' role
     * @throws java.sql.SQLException exception thrown during sql DELETE
     */
    fun deleteRecord(connection: Connection, userId: Long, contactId: Long) {
        InternalUsers.requireRole(connection, userId, "collection")
        val deleteCount = connection.runUpdate(
            sql = "DELETE FROM $tableName WHERE contact_id = ?",
            contactId,
        )
        if (deleteCount == 0) {
            throw NoRecordAffected(tableName, "Delete contact DML did not affect any records")
        }
    }

}
