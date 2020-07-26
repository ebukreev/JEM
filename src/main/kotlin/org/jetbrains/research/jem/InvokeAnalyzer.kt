package org.jetbrains.research.jem

import javassist.CtMethod
import javassist.bytecode.Opcode.*
import javassist.bytecode.analysis.ControlFlow

internal class InvokeAnalyzer(method: CtMethod,
                              private val blockAnalyzer: BlockAnalyzer) {

    private val iterator = method.methodInfo2.codeAttribute.iterator()
    private val constPool = method.methodInfo2.constPool
    private val classPool = method.declaringClass.classPool
    private val initsName = setOf("<init>", "<clinit>")

    private fun getInvokedMethods(block: ControlFlow.Block): List<Triple<String?, String, String>> {
        val methods = mutableListOf<Triple<String, String, String>>()
        val pos = block.position()
        val len = block.length()
        for (i in pos until pos + len) {
            when (iterator.byteAt(i)) {
                INVOKEVIRTUAL, INVOKESTATIC, INVOKESPECIAL -> {
                    val invokedMethod = iterator.u16bitAt(i + 1)
                    val className = constPool.getMethodrefClassName(invokedMethod)
                    val methodName = constPool.getMethodrefName(invokedMethod)
                    val methodDescriptor = constPool.getMethodrefType(invokedMethod)
                    methods.add(Triple(className, methodName, methodDescriptor))
                }
                INVOKEINTERFACE -> {
                    val invokedMethod = iterator.u16bitAt(i + 1)
                    val interfaceNameName = constPool.getInterfaceMethodrefClassName(invokedMethod)
                    val methodName = constPool.getInterfaceMethodrefName(invokedMethod)
                    val methodDescriptor = constPool.getInterfaceMethodrefType(invokedMethod)
                    methods.add(Triple(interfaceNameName, methodName, methodDescriptor))
                }
            }
        }
        return methods
    }

    fun getPossibleExceptionsFromMethods(block: ControlFlow.Block): Set<String> {
        val exceptions = mutableSetOf<String>()
        for ((c, m, d) in getInvokedMethods(block)) {
            if (c == null || c.contains("java.lang")) {
                continue
            }
            val `class` = classPool.get(c)
            val method = if (m in initsName)
                `class`.getConstructor(d).toMethod(m, classPool.get(c))
            else
                `class`.getMethod(m, d)
            if (ControlFlow(method).dominatorTree() == null) {
                continue
            }
            val methodAnalyzer = MethodAnalyzer(method)
            val possibleExceptions = methodAnalyzer
                    .getPossibleExceptions()
                    .takeWhile { !blockAnalyzer.isCaught(block, it) }
            exceptions.addAll(possibleExceptions)
        }
        return exceptions
    }
}