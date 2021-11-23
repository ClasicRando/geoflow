package api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sealed definition of any API response */
sealed interface ApiResponse {

    /** API response with a single serializable object in the payload */
    @Serializable
    data class SuccessSingle<T>(
        /** type of response object */
        @SerialName("object")
        val responseObject: String,
        /** payload of response as a single object */
        val payload: T,
    ): ApiResponse

    /** API response with a single serializable object in the payload */
    @Serializable
    data class SuccessMulti<T>(
        /** type of response object */
        @SerialName("object")
        val responseObject: String,
        /** payload of response as an array of objects */
        val payload: List<T>,
    ): ApiResponse

    /** API error response, contains details of error */
    @Serializable
    data class Error(
        /** error code */
        val code: Int,
        /** general message about error */
        val message: String,
        /** details about throwables from the error */
        val errors: ApiErrors,
    ): ApiResponse
}
