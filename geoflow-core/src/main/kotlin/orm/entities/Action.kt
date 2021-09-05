package orm.entities

import org.ktorm.entity.Entity

interface Action: Entity<Action> {
    val actionOid: Long
    val state: String
    val role: String
    val name: String
    val description: String
    val href: String
}