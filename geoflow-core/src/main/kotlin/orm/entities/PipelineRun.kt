package orm.entities

import org.ktorm.entity.Entity
import orm.enums.MergeType
import orm.enums.OperationState
import java.time.LocalDate

interface PipelineRun: Entity<PipelineRun> {
    val runId: Long
    val dataSource: DataSource
    val recordDate: LocalDate
    val workflowOperation: String
    val operationState: OperationState
    val collectionUser: InternalUser?
    val loadUser: InternalUser?
    val checkUser: InternalUser?
    val qaUser: InternalUser?
    val productionCount: Int
    val stagingCount: Int
    val matchCount: Int
    val newCount: Int
    val plottingStats: Map<String, Int>
    val hasChildTables: Boolean
    val mergeType: MergeType
}