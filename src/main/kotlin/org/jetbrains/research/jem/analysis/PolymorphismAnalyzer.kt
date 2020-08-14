package org.jetbrains.research.jem.analysis

import javassist.CtClass
import javassist.CtMethod
import javassist.NotFoundException
import org.jetbrains.research.jem.interaction.MethodInformation

class PolymorphismAnalyzer(private val classPool: Array<CtClass>) {

    private val heirs = getHeirsForClassesAndInterfaces()
    private val methodToOverriders = getOverriddenMethods()
    val methodToExceptions = getExceptions()

    private fun getHeirsForClassesAndInterfaces(): Map<CtClass, Set<CtClass>> {
        val heirs = mutableMapOf<CtClass, MutableSet<CtClass>>()
        try {
            for (c in classPool) {
                if (c.superclass != null) {
                    (heirs.getOrPut(c.superclass) { mutableSetOf() }).add(c)
                }
                if (c.interfaces.isNotEmpty()) {
                    for (`interface` in c.interfaces) {
                        (heirs.getOrPut(`interface`) { mutableSetOf() }).add(c)
                    }
                }
            }
        } catch (e: NotFoundException) {}
        return heirs
    }

    private fun getOverriddenMethods(): Map<MethodInformation, Set<CtMethod>> {
        val methodToOverriders = mutableMapOf<MethodInformation, MutableSet<CtMethod>>()
        for ((`class`, subclasses) in heirs) {
            `class`.methods.forEach { m ->
                val methodInformation = MethodInformation(m)
                subclasses.forEach { sc ->
                    (methodToOverriders.getOrPut(methodInformation) { mutableSetOf() })
                            .addAll(sc.methods.filter {
                                it.name == m.name &&
                                        it.methodInfo2.descriptor == m.methodInfo2.descriptor
                            })
                }
                if (methodToOverriders.getValue(methodInformation).isEmpty()) {
                    methodToOverriders[methodInformation] = mutableSetOf(m)
                }
            }
        }
        return methodToOverriders
    }

    private fun getExceptions(): Map<MethodInformation, Set<String>> =
        methodToOverriders.mapValues { (_, v) ->
            v.map {
                MethodAnalyzer(it).getPossibleExceptions()
            }.reduce { acc, set -> acc.intersect(set) }
        }
}