package orm.entities

import org.ktorm.entity.Entity
import orm.enums.FileCollectType
import orm.enums.LoaderType

interface SourceTable: Entity<SourceTable> {
    val stOid: Long
    val runId: Long
    val tableName: String
    val fileName: String
    val analyze: Boolean
    val load: Boolean
    val loaderType: LoaderType
    val qualified: Boolean
    val encoding: String
    val subTable: String?
    val recordCount: Int
    val fileId: String
    val url: String?
    val comments: String?
    val collectType: FileCollectType
    val delimiter: String?
}