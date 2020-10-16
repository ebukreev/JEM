package org.jetbrains.research.jem.interaction

import javassist.ClassPool
import javassist.CtClass
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        val packagesToClasses = mutableMapOf<String, MutableSet<CtClass>>()
        classes.map { (it.packageName ?: ".") to it }.forEach {
            packagesToClasses.getOrPut(it.first) { mutableSetOf() }.add(it.second)
        }
        for ((packageName, klasses) in packagesToClasses) {
            val classesForPackageEntity = mutableListOf<Class>()
            for (c in klasses) {
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
                    classesForPackageEntity.add(clazz)
                } catch (e: Exception) {
                    continue
                }
            }
            var path = System.getProperty("user.home") +
                    "/.JEMPluginCache"
            File(path).mkdir()
            for (part in packageName.split(".")) {
                path += "/$part"
                File(path).mkdir()
            }
            path += "/${packageName}.json"
            val pack = Package(packageName, classesForPackageEntity)
            val json = Json { prettyPrint = true }
            val packAsString = json.encodeToString(pack)
            FileWriter(path).use { it.write(packAsString) }
            MethodAnalyzer.previousMethods.clear()
        }
    }
}