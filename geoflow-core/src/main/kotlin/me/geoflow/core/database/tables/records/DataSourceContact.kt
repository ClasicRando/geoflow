package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.geoflow.core.database.tables.DataSourceContacts

/** API response record for the [DataSourceContacts] table */
@Serializable
data class DataSourceContact(
    /** Primary ID for a [DataSourceContacts] record */
    @SerialName("contact_id")
    val contactId: Long? = null,
    /** ID for the parent [DataSource] record */
    @SerialName("ds_id")
    val dsId: Long,
    /** Full name for the contact */
    val name: String?,
    /** Email address for the contact */
    val email: String?,
    /** Website linked to the contact */
    val website: String?,
    /** Type of contact. Should be better defined in the future with an enum */
    val type: String?,
    /** Miscellaneous notes about a contact */
    val notes: String?,
)
