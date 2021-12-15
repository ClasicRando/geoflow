package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Data class to represent a single database record for [Provs][me.geoflow.core.database.tables.Provs] */
@Serializable
data class Prov(
    /** 2-letter code for the prov/state value. Unique for all records */
    @SerialName("prov_code")
    val provCode: String,
    /** Full name for the prov/state */
    val name: String,
    /** Code for the country of the prov/state */
    @SerialName("country_code")
    val countryCode: String,
    /** Full name for the country of the prov/state */
    @SerialName("country_name")
    val countryName: String,
)
