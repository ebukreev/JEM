package org.jetbrains.research.jem

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