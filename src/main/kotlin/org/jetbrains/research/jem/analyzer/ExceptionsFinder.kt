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
    // create Jar file instance
    val jar = Jar(path, `package`)
    // create ClassManager and initialize it with the jar
    val cm = ClassManager(KfgConfig(Flags.readAll, failOnError = true))
    cm.initialize(jar)
    // iterate over all found classes
    for (klass in cm.concreteClasses) {
        //println(klass.name)
        for (method in klass.allMethods) {
            // view each method as graph
            //method.view("/usr/bin/dot", "/usr/bin/firefox")
            println(getThrownExceptions(method))
            //method.basicBlocks.forEach { println(it); println("\n---------------------------\n") }
        }
    }
    // save all changes to methods back to jar
    jar.update(cm)
}