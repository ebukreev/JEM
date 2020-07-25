package org.jetbrains.research.jem

import javassist.CtClass
import javassist.CtMethod
import javassist.bytecode.Opcode.*
import javassist.bytecode.analysis.ControlFlow

internal class InvokeAnalyzer(methodForAnalysis: CtMethod) {

    private val method = methodForAnalysis
    private val iterator = method.methodInfo2.codeAttribute.iterator()
    private val constPool = method.methodInfo2.constPool
    private val invokeOpcodes = setOf(
            INVOKEDYNAMIC,
            INVOKEINTERFACE,
            INVOKESPECIAL,
            INVOKESTATIC,
            INVOKEVIRTUAL
    )

    fun getInvokedMethods(block: ControlFlow.Block): List<Triple<String, String, String>> {
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
//                INVOKEDYNAMIC ->
//                    methods.add(constPool.getInvokeDynamicType(iterator.u16bitAt(i + 1)))
            }
        }
        return methods
    }
}