package orm.entities

import org.ktorm.entity.Entity

interface Prov: Entity<Prov> {
    val provCode: String
    val name: String
    val countryCode: String
    val countryName: String
}