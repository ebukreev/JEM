package org.jetbrains.research.jem.interaction

import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import javassist.CtClass
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.research.jem.analysis.PolymorphismAnalyzer
import org.jetbrains.research.jem.analysis.PolymorphismAnalyzer.Companion.reduceMethodsExceptions

object LazyPolymorphAnalyzer {

    private lateinit var heirs: Map<CtClass, Set<CtClass>>
    fun isInit() = ::heirs.isInitialized

    fun init(project: Project) {
        val jarsPaths = project.allModules().getJarsPaths()
        val allClasses = mutableListOf<CtClass>()
        jarsPaths.forEach {
            allClasses.addAll(JarAnalyzer.getClassesByJarPath(it))
        }
        heirs = PolymorphismAnalyzer
                .getHeirsForClassesAndInterfaces(allClasses.toTypedArray())
    }

    private fun List<Module>.getJarsPaths(): MutableSet<String> {
        val result = mutableSetOf<String>()
        this.forEach { m ->
            result.addAll(
                ModuleRootManager
                    .getInstance(m)
                    .orderEntries()
                    .classes()
                    .roots
                    .map { it.canonicalPath ?: "" }
                    .filter { it.endsWith("jar!/") }
                    .map { it.removeSuffix("!/") }
                    .toSet()
            )
        }
        return result
    }

    fun analyze(method: MethodInformation): Set<String>? {
        if (heirs.keys.map { it.name }.contains(method.clazz)) {
            val entry = heirs.entries.first { it.key.name == method.clazz }
            val methodToOverriders =
                PolymorphismAnalyzer.getOverriddenMethods(mapOf(entry.toPair()))
            if (methodToOverriders.containsKey(method)) {
                return methodToOverriders.getValue(method).reduceMethodsExceptions()
            }
        }
        return null
    }
}