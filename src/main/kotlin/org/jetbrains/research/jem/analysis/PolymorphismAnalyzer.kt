package org.jetbrains.research.jem.analysis

import javassist.CtClass
import javassist.CtMethod
import javassist.bytecode.Descriptor
import org.jetbrains.research.jem.interaction.MethodInformation

class PolymorphismAnalyzer(classPool: Array<CtClass>) {

    private val heirs = getHeirsForClassesAndInterfaces(classPool)
    private val methodToOverriders = getOverriddenMethods(heirs)
    val methodToExceptions = getExceptions(methodToOverriders)

    companion object {
        fun getHeirsForClassesAndInterfaces(classPool: Array<CtClass>): Map<CtClass, Set<CtClass>> {
            val heirs = mutableMapOf<CtClass, MutableSet<CtClass>>()
            for (c in classPool) {
                try {
                    if (c.superclass != null) {
                        (heirs.getOrPut(c.superclass) { mutableSetOf() }).add(c)
                    }
                    if (c.interfaces.isNotEmpty()) {
                        for (`interface` in c.interfaces) {
                            (heirs.getOrPut(`interface`) { mutableSetOf() }).add(c)
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            return heirs
        }

        fun getOverriddenMethods(heirs: Map<CtClass, Set<CtClass>>): Map<MethodInformation, List<CtMethod>> {
            val methodToOverriders = mutableMapOf<MethodInformation, MutableList<CtMethod>>()
            for ((clazz, subclasses) in heirs) {
                clazz.methods.forEach { m ->
                    val methodInformation = MethodInformation(m)
                    subclasses.forEach { sc ->
                        (methodToOverriders.getOrPut(methodInformation) { mutableListOf() })
                            .addAll(sc.methods.filter {
                                it.name == m.name &&
                                        !it.isEmpty &&
                                        Descriptor.eqParamTypes(m.signature, it.signature)
                            })
                    }
                    if (!m.isEmpty || methodToOverriders.getValue(methodInformation).isEmpty())
                        methodToOverriders.getValue(methodInformation).add(m)
                }
            }
            return methodToOverriders
        }

        fun getExceptions(methodToOverriders: Map<MethodInformation, List<CtMethod>>)
                : Map<MethodInformation, Set<String>> =
            methodToOverriders.mapValues { (_, v) ->
                v.reduceMethodsExceptions()
            }

        fun List<CtMethod>.reduceMethodsExceptions() =
            this.map {
                MethodAnalyzer(it).getPossibleExceptions()
            }.reduce { acc, set -> acc.intersect(set) }
    }
}