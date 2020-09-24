package org.jetbrains.research.jem.interaction

import com.google.gson.Gson
import org.jetbrains.research.jem.analysis.MethodAnalyzer
import java.io.File
import java.io.FileWriter

object JarAnalyzer {
    fun analyze(pathToJar: String) {
        val classes = PolyMethodsInitializer.addByJarPathAndGetClasses(pathToJar)
        PolyMethodsInitializer.initPolyMethods()
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
    }
}