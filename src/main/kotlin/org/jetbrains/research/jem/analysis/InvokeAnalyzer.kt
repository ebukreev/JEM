package org.jetbrains.research.jem.analysis

import javassist.CtMethod
import javassist.Modifier
import javassist.bytecode.Opcode.*
import javassist.bytecode.analysis.ControlFlow
import org.jetbrains.research.jem.interaction.MethodInformation

internal class InvokeAnalyzer(method: CtMethod,
                              private val blockAnalyzer: BlockAnalyzer) {

    private val iterator = method.methodInfo2.codeAttribute.iterator()
    private val constPool = method.methodInfo2.constPool
    private val classPool = method.declaringClass.classPool
    private val initsName = setOf("<init>", "<clinit>")

    private fun getInvokedMethods(block: ControlFlow.Block): List<MethodInformation> {
        val methods = mutableListOf<MethodInformation>()
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
                        methods.add(
                            MethodInformation(
                                className,
                                methodName,
                                methodDescriptor
                            )
                        )
                    }
                    INVOKEINTERFACE -> {
                        val invokedMethod = iterator.u16bitAt(i + 1)
                        val interfaceName = constPool.getInterfaceMethodrefClassName(invokedMethod)
                        val methodName = constPool.getInterfaceMethodrefName(invokedMethod)
                        val methodDescriptor = constPool.getInterfaceMethodrefType(invokedMethod)
                        methods.add(
                            MethodInformation(
                                interfaceName,
                                methodName,
                                methodDescriptor
                            )
                        )
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
                val clazz = classPool.get(c)
                val method = if (m in initsName)
                    clazz.getConstructor(d).toMethod(m, classPool.get(c))
                else
                    clazz.getMethod(m, d)
                if (Modifier.isNative(method.modifiers))
                    continue
                val methodInformation =
                    MethodInformation(method)
                if (MethodAnalyzer.polyMethodsExceptions.containsKey(methodInformation)) {
                    exceptions.addAll(
                        MethodAnalyzer.polyMethodsExceptions
                            .getValue(methodInformation)
                            .filter { !blockAnalyzer.isCaught(block, it) })
                    continue
                }
                if (MethodAnalyzer.previousMethods.containsKey(methodInformation)) {
                    exceptions.addAll(
                        MethodAnalyzer.previousMethods
                            .getValue(methodInformation)
                            .filter { !blockAnalyzer.isCaught(block, it) })
                    continue
                }
                val methodAnalyzer = MethodAnalyzer(method)
                val possibleExceptions = methodAnalyzer
                        .getPossibleExceptions()
                        .filter { !blockAnalyzer.isCaught(block, it) }
                exceptions.addAll(possibleExceptions)
            } catch (e: Exception) {
                continue
            }
        }
        return exceptions
    }
}