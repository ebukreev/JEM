package org.jetbrains.research.jem.interaction

import com.google.gson.Gson
import javassist.ClassPool
import org.jetbrains.research.jem.analysis.MethodAnalyzer
import org.jetbrains.research.jem.analysis.PolymorphismAnalyzer
import java.io.FileWriter
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile

object JarAnalyzer {
    fun analyze(pathToJar: String) {
        val pool = ClassPool.getDefault()
        pool.appendPathList(pathToJar)
        val classes = mutableListOf<String>()
        val file = JarFile(pathToJar)
        val entries: Enumeration<JarEntry> = file.entries()

        while (entries.hasMoreElements()) {
            val e: JarEntry = entries.nextElement()
            if (e.name.endsWith(".class") && !e.name.startsWith("META-INF"))
                classes.add(e.name.replace("/", ".").removeSuffix(".class"))
        }

        val cc = pool.get(classes.toTypedArray())
        MethodAnalyzer.initPolymorphismAnalyzer(
            PolymorphismAnalyzer(cc)
        )

        val classesForLibEntity = mutableListOf<Class>()
        for (c in cc) {
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
            val `class` = Class(c.name, methodsForClassEntity)
            classesForLibEntity.add(`class`)
        }

        val libName = pathToJar.substringAfterLast("/").removeSuffix(".jar")
        val lib = Library(libName, classesForLibEntity)
        val filePath = "./analyzedLibs/$libName.json"
        FileWriter(filePath).use { Gson().toJson(lib, it) }
    }
}