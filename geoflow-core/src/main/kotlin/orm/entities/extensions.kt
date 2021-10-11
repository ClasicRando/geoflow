package orm.entities

import formatLocalDateDefault

val PipelineRun.runFilesLocation: String
    get() = "${this.dataSource.filesLocation}/${formatLocalDateDefault(this.recordDate)}/files"

val PipelineRun.runZipLocation: String
    get() = "${this.dataSource.filesLocation}/${formatLocalDateDefault(this.recordDate)}/zip"