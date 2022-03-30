package me.geoflow.core.database.composites

import org.postgresql.util.PGobject
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

/** */
private fun getPostgresTypeFromKClass(kType: KType): String {
    val kClass = kType.jvmErasure
    return when {
        kClass.isSubclassOf(Short::class) -> "smallint"
        kClass.isSubclassOf(Int::class) -> "integer"
        kClass.isSubclassOf(Long::class) -> "bigint"
        kClass.isSubclassOf(String::class) -> "text"
        kClass.isSubclassOf(Double::class) -> "double precision"
        kClass.isSubclassOf(Float::class) -> "real"
        kClass.isSubclassOf(Instant::class) -> "timestamp"
        kClass.isSubclassOf(PGobject::class) -> getCompositeName(kClass)
        kClass.isSubclassOf(List::class) -> {
            val listTypeClass = Regex("(?<=<).+(?=>)").find(kType.toString())?.value
                ?: error("Could not find a proper generic type for $kType")
            val jClass = Class.forName(listTypeClass)
            "${getCompositeName(jClass.kotlin)}[]"
        }
        else -> error("Class ${kClass.simpleName} is not registered. Cannot find a postgresql equivalent")
    }
}

/** */
fun getCompositeDefinition(kClass: KClass<*>): String {
    require(kClass.hasAnnotation<Composite>()) {
        "The KClass provided (${kClass.jvmName}) does not have a CompositeAnnotation annotation"
    }
    require(kClass.isData) { "${kClass.simpleName} is not a data class. Only data classes can be composite classes" }
    val params = kClass.primaryConstructor!!.parameters
    val properties = kClass.memberProperties
    val zip = params.map { param -> param to properties.first { it.name == param.name } }
    return zip.joinToString(
        prefix = "CREATE TYPE public.${getCompositeName(kClass)} AS (",
        postfix = ");",
    ) { (param, property) ->
        val paramType = param.type
        val propertyType = property.returnType
        require(propertyType == paramType) {
            """
            Mismatch between property and parameter for ${param.name}
            Parameter: $paramType
            Property: $propertyType
        """.trimIndent()
        }
        val propertyName = (property.javaField?.annotations?.firstOrNull {
            it.annotationClass.isSubclassOf(CompositeField::class)
        } as? CompositeField)?.name ?: property.name
        "$propertyName ${getPostgresTypeFromKClass(paramType)}"
    }
}

/** */
fun getCompositeName(kClass: KClass<*>): String {
    val annotation = kClass.annotations.firstOrNull {
        it.annotationClass.isSubclassOf(Composite::class)
    }
    requireNotNull(annotation) {
        "The Type provided (${kClass.jvmName}) does not have a Composite annotation"
    }
    return (annotation as Composite).name
}

/** */
inline fun <reified T> getCompositeName(): String {
    val annotation = T::class.annotations.firstOrNull {
        it.annotationClass.isSubclassOf(Composite::class)
    }
    requireNotNull(annotation) {
        "The Type provided (${T::class.jvmName}) does not have a Composite annotation"
    }
    return (annotation as Composite).name
}
