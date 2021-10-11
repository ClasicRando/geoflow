package orm.tables

import java.io.InputStream

interface DefaultData {
    val defaultRecordsFileName: String
}

val DefaultData.defaultRecordsFile: InputStream?
    get() = this::class.java.classLoader.getResourceAsStream(defaultRecordsFileName)