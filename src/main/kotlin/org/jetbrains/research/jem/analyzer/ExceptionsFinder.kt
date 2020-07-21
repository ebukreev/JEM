package org.jetbrains.research.jem.analyzer

import org.jetbrains.research.kfg.*
import org.jetbrains.research.kfg.util.*
import java.nio.file.Path

fun main() {
    val jar = Jar(Path.of("./demo/Main.jar"), Package("*"))
    val cm = ClassManager(KfgConfigBuilder().build())
    cm.initialize(jar)
    //pipelineExample(cm, Package("*"))
    example(Path.of("./demo/Main.jar"), Package("*"))
}

fun example(path: Path, `package`: Package) {
    val jar = Jar(path, `package`)
    val cm = ClassManager(KfgConfig(Flags.readAll, failOnError = true))
    cm.initialize(jar)
    for (klass in cm.concreteClasses) {
        //println(klass.name)
        for (method in klass.allMethods) {
            //method.view("/usr/bin/dot", "/usr/bin/firefox")
            println(MethodAnalyzer.getThrownExceptions(method))
            //method.catchEntries.forEach { println(it.successors); println("\n---------------------------\n") }
        }
    }
    jar.update(cm)
}