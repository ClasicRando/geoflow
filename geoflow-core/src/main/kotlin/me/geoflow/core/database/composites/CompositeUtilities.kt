package me.geoflow.core.database.composites

import org.postgresql.util.PGobject
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

/** */
private fun getPostgresTypeFromKClass(kType: KType): Pair<String, Boolean> {
    val kClass = kType.jvmErasure
    return when {
        kClass.isSubclassOf(Short::class) -> "smallint" to false
        kClass.isSubclassOf(Int::class) -> "integer" to false
        kClass.isSubclassOf(Long::class) -> "bigint" to false
        kClass.isSubclassOf(String::class) -> "text" to false
        kClass.isSubclassOf(Double::class) -> "double precision" to false
        kClass.isSubclassOf(Float::class) -> "real" to false
        kClass.isSubclassOf(Instant::class) -> "timestamp" to false
        kClass.isSubclassOf(PGobject::class) -> getCompositeName(kClass) to true
        kClass.isSubclassOf(List::class) -> {
            val listTypeClass = Regex("(?<=<).+(?=>)").find(kType.toString())?.value
                ?: error("Could not find a proper generic type for $kType")
            val jClass = Class.forName(listTypeClass)
            val (type, isComposite) = getPostgresTypeFromKClass(jClass.kotlin.starProjectedType)
            "$type[]" to isComposite
        }
        else -> error("Class ${kClass.simpleName} is not registered. Cannot find a postgresql equivalent")
    }
}

/** */
fun getCompositeDefinition(kClass: KClass<*>): CompositeDefinition {
    require(kClass.hasAnnotation<Composite>()) {
        "The KClass provided (${kClass.jvmName}) does not have a CompositeAnnotation annotation"
    }
    require(kClass.isData) { "${kClass.simpleName} is not a data class. Only data classes can be composite classes" }
    val params = kClass.primaryConstructor!!.parameters
    val properties = kClass.memberProperties
    val compositeName = getCompositeName(kClass)
    val subComposites = mutableSetOf<String>()
    val createStatement = params.map { param ->
        param to properties.first { it.name == param.name }
    }.joinToString(
        prefix = "CREATE TYPE public.${compositeName} AS (",
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
        val (type, isComposite) = getPostgresTypeFromKClass(paramType)
        if (isComposite) {
            subComposites += type.trim('[',']')
        }
        "$propertyName $type"
    }
    return CompositeDefinition(
        name = compositeName,
        createStatement = createStatement,
        subComposites = subComposites,
    )
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
