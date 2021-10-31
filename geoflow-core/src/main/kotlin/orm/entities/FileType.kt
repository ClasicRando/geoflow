package orm.entities

import org.ktorm.entity.Entity
import orm.enums.LoaderType

interface FileType: Entity<FileType> {
    val fileExtension: String
    val loaderType: LoaderType
}