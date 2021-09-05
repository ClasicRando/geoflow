package orm.entities

import org.ktorm.entity.Entity

interface DataSource: Entity<DataSource> {
    val dsId: Long
    val code: String
    val country: String
    val prov: String
    val description: String
    val filesLocation: String
    val provLevel: Boolean
}