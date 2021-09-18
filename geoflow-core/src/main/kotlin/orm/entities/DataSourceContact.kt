package orm.entities

import org.ktorm.entity.Entity

interface DataSourceContact: Entity<DataSourceContact> {
    val contactId: Long
    val dsId: Long
    val name: String?
    val email: String?
    val website: String?
    val type: String?
    val notes: String?
}