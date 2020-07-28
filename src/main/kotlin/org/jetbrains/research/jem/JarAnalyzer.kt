package org.jetbrains.research.jem

import javassist.ClassPool
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
        for (c in cc) {
            println(c.name)
            val methods = c.methods
            for (m in methods) {
                println (m.name)
                val analyser = MethodAnalyzer(m)
                println(analyser.getPossibleExceptions())
            }
        }
    }
}