package orm.entities

import org.ktorm.entity.Entity

interface InternalUser: Entity<InternalUser> {
    val userOid: Long
    val name: String
    val username: String
    val password: String
    val roles: Array<String?>
}