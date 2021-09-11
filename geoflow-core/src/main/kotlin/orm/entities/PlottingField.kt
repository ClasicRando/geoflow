package orm.entities

import org.ktorm.entity.Entity

interface PlottingField: Entity<PlottingField> {
    val name: String
    val addressLine1: String
    val addressLine2: String
    val city: String
    val alternativeCities: Array<String?>
    val dsId: Long
    val mailCode: String
    val latitude: String
    val longitude: String
    val prov: String
    val fileId: String
    val cleanAddress: String
    val cleanCity: String
}