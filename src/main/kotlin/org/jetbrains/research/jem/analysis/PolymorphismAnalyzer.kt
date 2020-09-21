package org.jetbrains.research.jem.analysis

import com.jetbrains.rd.framework.base.deepClonePolymorphic
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.bytecode.Descriptor
import org.jetbrains.research.jem.interaction.MethodInformation

class PolymorphismAnalyzer(private val classPool: Array<CtClass>) {

    // TODO: Consider possible additions to "heirs"
    //       from classes in other jars from our closed world
    //       (maybe run the analysis over fat or uber jar?)

    private val heirs = getHeirsForClassesAndInterfaces()
    private val methodToOverriders = getOverriddenMethods()
    val methodToExceptions = getExceptions()

    private fun getHeirsForClassesAndInterfaces(): Map<CtClass, Set<CtClass>> {
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

    private fun getOverriddenMethods(): Map<MethodInformation, Set<CtMethod>> {
        val methodToOverriders = mutableMapOf<MethodInformation, MutableSet<CtMethod>>()
        for ((clazz, subclasses) in heirs) {
            clazz.methods.forEach { m ->
                val methodInformation = MethodInformation(m)
                subclasses.forEach { sc ->
                    (methodToOverriders.getOrPut(methodInformation) { mutableSetOf() })
                            .addAll(sc.methods.filter {
                               it.name == m.name &&
                                       !it.isEmpty &&
                                       Descriptor.eqParamTypes(m.signature, it.signature) &&
                                        it.returnType.subtypeOf(m.returnType)
                            })
                }
                if (!m.isEmpty || methodToOverriders.getValue(methodInformation).isEmpty())
                    methodToOverriders.getValue(methodInformation).add(m)
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