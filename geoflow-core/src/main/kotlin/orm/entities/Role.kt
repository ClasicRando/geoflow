package orm.entities

import org.ktorm.entity.Entity

interface Role: Entity<Role> {
    val name: String
    val description: String
}