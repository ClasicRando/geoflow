package orm.entities

import org.ktorm.entity.Entity

interface SourceTableColumn: Entity<SourceTableColumn> {
    val stcOid: Long
    val name: String
    val type: String
    val maxLength: Int
    val minLength: Int
    val label: String
    val stOid: Long
}