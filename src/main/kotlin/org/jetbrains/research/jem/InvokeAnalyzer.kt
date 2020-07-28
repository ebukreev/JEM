package org.jetbrains.research.jem

import javassist.CtMethod
import javassist.Modifier
import javassist.bytecode.Opcode.*
import javassist.bytecode.analysis.ControlFlow

internal class InvokeAnalyzer(method: CtMethod,
                              private val blockAnalyzer: BlockAnalyzer) {

    private val iterator = method.methodInfo2.codeAttribute.iterator()
    private val constPool = method.methodInfo2.constPool
    private val classPool = method.declaringClass.classPool
    private val initsName = setOf("<init>", "<clinit>")

    companion object {
        internal fun exactMethodName(method: CtMethod): String =
                method.declaringClass.name + " " +
                        method.name + " " +
                        method.methodInfo2.descriptor
    }

    private fun getInvokedMethods(block: ControlFlow.Block): List<Triple<String?, String, String>> {
        val methods = mutableListOf<Triple<String, String, String>>()
        val pos = block.position()
        val len = block.length()
        for (i in pos until pos + len) {
            try {
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
            } catch (e: Exception) {
                continue
            }
        }
        return methods
    }

    fun getPossibleExceptionsFromMethods(block: ControlFlow.Block): Set<String> {
        val exceptions = mutableSetOf<String>()
        val invokedMethods = getInvokedMethods(block)
        for ((c, m, d) in invokedMethods) {
            if (c == null) {
                continue
            }
            try {
                val `class` = classPool.get(c)
                val method = if (m in initsName)
                    `class`.getConstructor(d).toMethod(m, classPool.get(c))
                else
                    `class`.getMethod(m, d)
                if (Modifier.isNative(method.modifiers))
                    continue
                if (exactMethodName(method) in MethodAnalyzer.previousMethods.keys) {
                    exceptions.addAll(MethodAnalyzer.previousMethods[exactMethodName(method)]!!
                            .takeWhile { !blockAnalyzer.isCaught(block, it) })
                    continue
                }
                val methodAnalyzer = MethodAnalyzer(method)
                val possibleExceptions = methodAnalyzer
                        .getPossibleExceptions()
                        .takeWhile { !blockAnalyzer.isCaught(block, it) }
                exceptions.addAll(possibleExceptions)
            } catch (e: Exception) {
                continue
            }
        }
        return exceptions
    }
}