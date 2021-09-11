package orm.entities

import org.ktorm.entity.Entity

interface PlottingMethodType: Entity<PlottingMethodType> {
    val name: String
    val methodId: Int
}