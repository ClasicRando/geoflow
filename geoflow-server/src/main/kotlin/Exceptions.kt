import kotlin.reflect.full.createType

class UnauthorizedRouteAccessException(val route: String): Exception()

fun require(value: Boolean, throwable: () -> Throwable) {
    if (!value) {
        throwable()
    }
}

@JvmName("requireGeneric")
inline fun <reified T: Throwable> require(value: Boolean, message: () -> String) {
    if (!value) {
        val constructor = T::class.constructors
            .firstOrNull { it.parameters.size == 1 && it.parameters[0].type == String::class.createType() }
            ?: throw IllegalArgumentException("Defined Throwable does not have a single String parameter constructor")
        throw constructor.call(message())
    }
}