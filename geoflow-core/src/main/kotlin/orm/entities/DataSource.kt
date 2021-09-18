package orm.entities

import org.ktorm.entity.Entity
import java.time.Instant

interface DataSource: Entity<DataSource> {
    val dsId: Long
    val code: String
    val country: String
    val prov: String
    val description: String
    val filesLocation: String
    val provLevel: Boolean
    val comments: String?
    val assignedUser: InternalUser
    val created: Instant
    val createdBy: InternalUser
    val lastUpdated: Instant?
    val updatedBy: InternalUser?
    val searchRadius: Double
    val recordWarehouseType: RecordWarehouseType
    val reportingType: String
}