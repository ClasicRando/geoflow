package orm.entities

import formatLocalDateDefault

val PipelineRun.runFilesLocation: String
    get() = "${this.dataSource.filesLocation}/${formatLocalDateDefault(this.recordDate)}/files"