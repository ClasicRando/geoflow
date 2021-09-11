package orm.entities

import org.ktorm.entity.Entity

interface PlottingMethod: Entity<PlottingMethod> {
    val dsId: Long
    val order: Short
    val methodType: PlottingMethodType
    val fileId: String
}