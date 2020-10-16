package org.jetbrains.research.jem.interaction

import javassist.ClassPool
import javassist.CtMethod
import org.jetbrains.research.jem.analysis.MethodAnalyzer
import kotlinx.serialization.*

@Serializable
data class Package(
        val packageName: String,
        val classes: List<Class>
)

@Serializable
data class Class(
        val className: String,
        val methods: List<Method>
)

@Serializable
data class Method(
        val methodName: String,
        val descriptor: String,
        val exceptionsInfo: ExceptionsAndCalls
)

@Serializable
data class ExceptionsAndCalls(
        val exceptions: MutableSet<String>,
        val calls: MutableMap<String, Set<String>>,
        val allExceptions: MutableSet<String>
) {
        companion object {
                fun empty() =
                        ExceptionsAndCalls(
                                mutableSetOf(),
                                mutableMapOf(),
                                mutableSetOf()
                        )
        }
}

@Serializable
data class MethodInformation(
        val clazz: String?,
        val name: String,
        val descriptor: String
) {
    constructor(method: CtMethod): this(method.declaringClass.name, method.name, method.methodInfo2.descriptor)

        override fun toString() = "$clazz $name $descriptor"
}

fun String.toMethodInfo(): MethodInformation {
        val splitedString = this.split(" ")
        return MethodInformation(
                splitedString[0],
                splitedString[1],
                splitedString[2]
        )
}