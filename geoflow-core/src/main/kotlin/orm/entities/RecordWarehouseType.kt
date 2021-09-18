package orm.entities

import org.ktorm.entity.Entity

interface RecordWarehouseType: Entity<RecordWarehouseType> {
    val id: Int
    val name: String
    val description: String
}