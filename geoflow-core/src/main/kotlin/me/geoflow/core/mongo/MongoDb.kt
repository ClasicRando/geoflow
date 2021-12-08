package me.geoflow.core.mongo

import it.justwrote.kjob.job.JobStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.newId
import org.litote.kmongo.reactivestreams.KMongo
import java.time.Instant
import java.util.*

/** Connection to the MongoDB service holding the persistent jobs */
object MongoDb {

    /** Structure of a progress object in kjob */
    @Serializable
    data class JobProgress(
        /** */
        val step: Long,
        /** */
        val max: Long? = null,
        /** */
        @SerialName("started_at")
        @Contextual
        val startedAt: Instant? = null,
        /** */
        @SerialName("completed_at")
        @Contextual
        val completedAt: Instant? = null
    )

    /** Structure of the settings of a kjob record */
    @Serializable
    data class JobSettings(
        /** */
        @SerialName("_id")
        val id: String,
        /** */
        val name: String,
        /** */
        val properties: TaskProperties,
    )

    /** Structure of a [SystemTask][me.geoflow.core.tasks.SystemTask] */
    @Serializable
    data class TaskProperties(
        /** */
        val pipelineRunTaskId: Long,
        /** */
        val runId: Long,
        /** */
        val runNext: Boolean,
    )

    /** Base structure of a scheduled job in the kjob repository */
    @Serializable
    data class ScheduledJob(
        /** */
        @Contextual
        @SerialName("_id")
        val id: Id<ScheduledJob> = newId(),
        /** */
        val status: JobStatus,
        /** */
        @SerialName("run_at")
        @Contextual
        val runAt: Instant?,
        /** */
        val retries: Int,
        /** */
        @SerialName("kjob_id")
        @Contextual
        val kjobId: UUID?,
        /** */
        @SerialName("created_at")
        @Contextual
        val createdAt: Instant,
        /** */
        @SerialName("updated_at")
        @Contextual
        val updatedAt: Instant,
        /** */
        val settings: JobSettings,
        /** */
        val progress: JobProgress,
    )

    /** Structure of a request to the jobs collection */
    @Serializable
    data class TaskApiRequest(
        /** */
        val status: JobStatus,
    )

    private val client = KMongo.createClient().coroutine
    private val jobDatabase = client.getDatabase("kjob")
    private val jobCollection = jobDatabase.getCollection<ScheduledJob>("kjob-jobs")
//    private val lockCollection = jobDatabase.getCollection<Lock>("kjob-locks")

    /** Returns a [Flow] of all task jobs found in the 'kjob' collection that match the provided request [JobStatus] */
    fun getTasks(request: TaskApiRequest): Flow<ScheduledJob> {
        return jobCollection.find("{ status: '${request.status}' }").toFlow()
    }

}
