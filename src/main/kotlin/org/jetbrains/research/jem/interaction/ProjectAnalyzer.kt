package org.jetbrains.research.jem.interaction

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.kotlin.idea.util.projectStructure.allModules

class ProjectAnalyzer(project: Project) {
    init {
        val modules = project.allModules()
        val jarsPaths = mutableSetOf<String>()
        modules.forEach { m ->
            jarsPaths.addAll(
                ModuleRootManager
                .getInstance(m)
                .orderEntries()
                .classes()
                .roots
                    .map { it.canonicalPath ?: "" }
                    .filter { it.endsWith("jar!/") }
                    .map { it.replaceAfterLast(".jar", "") }
                    .toSet()
            )
        }
        PolyMethodsInitializer.addByJarPathAndGetClasses(jarsPaths)
        PolyMethodsInitializer.initPolyMethods()
    }

    fun analyze(pathToJar: String) =
        JarAnalyzer.analyze(pathToJar)
}