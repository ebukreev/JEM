package org.jetbrains.research.jem.interaction

import javassist.CtMethod

data class Library(
        val libName: String,
        val classes: List<Class>
)

data class Class(
        val className: String,
        val methods: List<Method>
)

data class Method(
        val methodName: String,
        val descriptor: String,
        val exceptions: Set<String>
)

data class MethodInformation(
        val `class`: String?,
        val name: String,
        val descriptor: String
) {
    constructor(method: CtMethod): this(method.declaringClass.name, method.name, method.name)
}