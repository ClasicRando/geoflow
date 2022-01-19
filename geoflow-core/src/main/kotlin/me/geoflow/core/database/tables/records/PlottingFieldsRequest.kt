package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API response body for [PlottingFields][me.geoflow.core.database.tables.PlottingFields] */
@Serializable
data class PlottingFieldsRequest(
    /** Unique ID for the source table that owns this record */
    @SerialName("st_oid")
    val stOid: Long,
    /** Merge key source field. Used to link to other tables to merge data */
    @SerialName("merge_key")
    val mergeKey: String?,
    /** Company name source field */
    val name: String?,
    /** Address1 source field */
    @SerialName("address_line1")
    val addressLine1: String?,
    /** Address2 source field */
    @SerialName("address_line2")
    val addressLine2: String?,
    /** City source field */
    val city: String?,
    /** Alternate city (e.g. county, municipality, township, etc.) source fields */
    @SerialName("alternate_cities")
    val alternateCities: List<String>?,
    /** Mail code (e.g. zip, postal code, etc.) source field */
    @SerialName("mail_code")
    val mailCode: String?,
    /** Latitude source field */
    val latitude: String?,
    /** Longitude source field */
    val longitude: String?,
    /** Prov (e.g. state, province, etc.) source field */
    val prov: String?,
    /** Cleaned/parsed version of address for geocoding purposes source field */
    @SerialName("clean_address")
    val cleanAddress: String?,
    /** Cleaned/parsed version of city for geocoding purposes source field */
    @SerialName("clean_city")
    val cleanCity: String?,
)