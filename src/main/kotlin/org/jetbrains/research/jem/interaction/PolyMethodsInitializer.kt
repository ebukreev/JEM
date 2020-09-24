package org.jetbrains.research.jem.interaction

import javassist.ClassPool
import javassist.CtClass
import org.jetbrains.research.jem.analysis.MethodAnalyzer
import org.jetbrains.research.jem.analysis.PolymorphismAnalyzer
import java.util.jar.JarFile

object PolyMethodsInitializer {

    private val classes = mutableSetOf<CtClass>()
    private val classPool = ClassPool.getDefault()

    fun initPolyMethods() {
        if (!MethodAnalyzer.polyMethodsExceptionsIsInitialized()) {
            MethodAnalyzer.polyMethodsExceptions =
                PolymorphismAnalyzer(classes.toTypedArray()).methodToExceptions.toMutableMap()
        } else {
            MethodAnalyzer.polyMethodsExceptions
                .putAll(PolymorphismAnalyzer(classes.toTypedArray()).methodToExceptions)
        }
    }

    fun addByJarPathAndGetClasses(jarsPaths: Iterable<String>): Array<CtClass> {
        val classes = mutableListOf<CtClass>()
        jarsPaths.forEach {
            classes.addAll(addByJarPathAndGetClasses(it))
        }
        return classes.toTypedArray()
    }


    fun addByJarPathAndGetClasses(jarPath: String): Array<CtClass> {
        classPool.appendPathList(jarPath)
        val file = JarFile(jarPath)
        val entries = file.entries()
        val classes = classPool.get(
            entries.asSequence().filter { e ->
                e.name.endsWith(".class") && !e.name.startsWith("META-INF")
            }.map { e ->
                e.name.replace("/", ".").removeSuffix(".class")
            }.toList().toTypedArray()
        )
        addClasses(classes.toList())
        return classes
    }

    private fun addClasses(classesToAdd: Iterable<CtClass>) =
        classes.addAll(classesToAdd)
}