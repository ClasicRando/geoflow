@file:Suppress("MatchingDeclarationName")
package errors

import utils.requireEmpty
import kotlin.reflect.full.primaryConstructor

/** Require contract that throws any [Throwable] the user wants by providing a lambda that returns any Throwable. */
fun require(value: Boolean, block: () -> Throwable) {
    if (!value) {
        val error = block()
        throw error
    }
}

/** Require contract that throws any [Throwable] that has an empty constructor when the provided value is false */
inline fun <reified T: Throwable> requireOrThrow(value: Boolean) {
    if (!value) {
        val constructor = T::class.primaryConstructor
        requireNotNull(constructor) { "Throwable must have constructor" }
        requireEmpty(constructor.parameters) { "Throwable's constructor must accept 0 parameters" }
        throw constructor.call()
    }
}
