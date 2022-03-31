package me.geoflow.core.database.enums

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Enum type found in DB denoting run type of task
 * CREATE TYPE public.task_run_type AS ENUM
 *  ('User', 'System');
 */
@Serializable(TaskRunType.TaskRunTypeSerializer::class)
sealed class TaskRunType(runType: String) : PgEnum("task_run_type", runType) {

    /** Task run with no action. Task is simply a logical interrupt to warn the user of something */
    object User : TaskRunType(user)

    /** Task run with an action. Task runs some underlining code as part of the pipeline */
    object System: TaskRunType(system)

    companion object {
        /** */
        fun fromString(status: String): TaskRunType {
            return when (status) {
                user -> User
                system -> System
                else -> error("Could not find a task_run_type for '$status'")
            }
        }
        private const val user = "User"
        private const val system = "System"
    }

    /** */
    object TaskRunTypeSerializer : KSerializer<TaskRunType> {
        /** */
        override fun deserialize(decoder: Decoder): TaskRunType {
            return fromString(decoder.decodeString())
        }

        /** */
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            serialName = "TaskRunType",
            kind = PrimitiveKind.STRING
        )

        /** */
        override fun serialize(encoder: Encoder, value: TaskRunType) {
            encoder.encodeString(value.value!!)
        }

    }

}
