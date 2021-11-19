import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

/** Exception thrown when a user requests a route that they are not authorized to access. */
class UnauthorizedRouteAccessException(val route: String): Exception()

/** Require contract that throws any [Throwable] the user wants by providing a lambda that returns any Throwable. */
fun require(value: Boolean, throwable: () -> Throwable) {
    if (!value) {
        val error = throwable()
        throw error
    }
}

/**
 * Require contract that throws any [Throwable] defined as a generic Throwable type that accepts a single String
 * parameter as the exception message. The message is obtained using a lazy [lambda][lazyMessage] that is only evaluated
 * if the value is false.
 */
@JvmName("requireGeneric")
inline fun <reified T: Throwable> require(value: Boolean, lazyMessage: () -> String) {
    if (!value) {
        val type = typeOf<T>()
        val constructor = T::class.constructors
            .firstOrNull { it.parameters.size == 1 && type.isSubtypeOf(it.parameters[0].type) }
            ?: throw IllegalArgumentException("Defined Throwable does not have a single String parameter constructor")
        val message = lazyMessage()
        throw constructor.call(message)
    }
}