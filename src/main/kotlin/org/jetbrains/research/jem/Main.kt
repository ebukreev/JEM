package org.jetbrains.research.jem

fun main() {
//    JarAnalyzer.analyze("./jarsfortest/soot.jar")
    val lib = InfoReader.read("./analyzedLibs/kfg.json")
    for (klass in lib.classes) {
        println(klass.className)
        for (method in klass.methods) {
            println(method.methodName)
            println(method.exceptions)
        }
    }
}

