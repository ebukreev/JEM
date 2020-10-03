package org.jetbrains.research.jem.interaction

import com.google.gson.Gson
import javassist.ClassPool
import javassist.CtClass
import org.jetbrains.research.jem.analysis.MethodAnalyzer
import org.jetbrains.research.jem.analysis.PolymorphismAnalyzer
import java.io.File
import java.io.FileWriter
import java.util.jar.JarFile

object JarAnalyzer {

    fun getClassesByJarPath(pathToJar: String): Array<CtClass> {
        val classPool = ClassPool.getDefault().apply { appendPathList(pathToJar) }
        val file = JarFile(pathToJar)
        val entries = file.entries()
        return classPool.get(
            entries.asSequence().filter { e ->
                e.name.endsWith(".class") && !e.name.startsWith("META-INF")
            }.map { e ->
                e.name.replace("/", ".").removeSuffix(".class")
            }.toList().toTypedArray()
        )
    }

    fun analyze(pathToJar: String, needInitPolyMethods: Boolean = true) {
        val classes = getClassesByJarPath(pathToJar)
        if (needInitPolyMethods) {
            MethodAnalyzer.polyMethodsExceptions =
                PolymorphismAnalyzer(classes).methodToExceptions.toMutableMap()
        }
        val classesForLibEntity = mutableListOf<Class>()
        for (c in classes) {
            try {
                val methodsForClassEntity = mutableListOf<Method>()
                val methods = c.methods
                for (m in methods) {
                    val analyser = MethodAnalyzer(m)
                    val method =
                        Method(
                            m.name,
                            m.methodInfo2.descriptor,
                            analyser.getPossibleExceptions()
                        )
                    methodsForClassEntity.add(method)
                }
                val clazz = Class(c.name, methodsForClassEntity)
                classesForLibEntity.add(clazz)
            } catch (e: Exception) {
                continue
            }
        }
        val libName = pathToJar.substringAfterLast("/").removeSuffix(".jar")
        val lib = Library(libName, classesForLibEntity)
        val filePath = "${System.getProperty("user.home")}/.JEMPluginСache/$libName.json"
        File("${System.getProperty("user.home")}/.JEMPluginСache").mkdir()
        FileWriter(filePath).use { Gson().toJson(lib, it) }
        MethodAnalyzer.previousMethods.clear()
    }
}