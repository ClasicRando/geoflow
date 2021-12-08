package me.geoflow.api.paths

import io.ktor.routing.Route
import io.ktor.routing.route

/** Base implementation of a path within the API. Provides ability to register endpoints and generate path for API */
abstract class ApiPath(private val path: String) {

    /** Function to register  */
    abstract fun Route.registerEndpoints()

    /** */
    fun generatePath(parent: Route) {
        parent.route(path = path) {
            registerEndpoints()
        }
    }
}
