package orm.entities

import org.ktorm.entity.Entity

interface SourceTable: Entity<SourceTable> {
    val stOid: Long
    val runId: Long
    val tableName: String
    val fileName: String
    val analyze: Boolean
    val load: Boolean
    val fileType: String
    val qualified: Boolean
    val encoding: String
    val subTable: String?
    val recordCount: Int
    val fileId: String
    val url: String
    val comments: String?
}