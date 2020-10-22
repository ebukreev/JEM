package org.jetbrains.research.jem.analysis

import javassist.CtBehavior
import javassist.Modifier
import javassist.bytecode.Opcode
import javassist.bytecode.analysis.ControlFlow

internal class BlockAnalyzer(method: CtBehavior) {

    private val iterator = method.methodInfo2.codeAttribute.iterator()
    private val cfg = ControlFlow(method.declaringClass, method.methodInfo2)
    private val classPool = MethodAnalyzer.classPool
    internal val reachableCatchBlocks = mutableSetOf<ControlFlow.Block>()
    private val constPool = method.methodInfo2.constPool
    private val initsName = setOf("<init>", "<clinit>")
    internal var hasEmptyFinally = false
    private val isKotlin = method.declaringClass.isKotlin
    private val invokeOpcodes = setOf(
            Opcode.INVOKEINTERFACE,
            Opcode.INVOKESPECIAL,
            Opcode.INVOKEVIRTUAL,
            Opcode.INVOKESTATIC
    )

    fun getThrownExceptions(block: ControlFlow.Block): Set<String> {
        val exceptions = mutableSetOf<String>()
        val pos = block.position()
        val len = block.length()
        for (i in pos until pos + len) {
            if (iterator.byteAt(i) == Opcode.ATHROW) {
                val exception = try {
                    cfg.frameAt(i).getStack(if (isKotlin) 1 else 0)
                } catch (e: Exception) {
                    continue
                }

                val exceptionAsString = exception.toString()

                if (!isCaught(block, exceptionAsString)) {
                    exceptions.add(exceptionAsString)
                }
            }
        }
        return exceptions
    }

    internal fun isCaught(block: ControlFlow.Block, exception: String): Boolean {
        block.catchers().forEach {
            if (isSubclass(exception, it.type()) &&
                    !isEmptyFinallyBlock(it) &&
                    it.block() != block) {
                reachableCatchBlocks.add(it.block())
                return true
            }
        }
        return false
    }

    private fun isSubclass(firstClass: String, secondClass: String) =
            classPool.get(firstClass)
                    .subclassOf(classPool.get(secondClass))

    private fun isEmptyFinallyBlock(catcher: ControlFlow.Catcher): Boolean =
            catcher.type() == "java.lang.Throwable" &&
                    hasThrowThrowable(catcher)

    private fun hasThrowThrowable(catcher: ControlFlow.Catcher): Boolean {
        val pos = catcher.block().position()
        val len = catcher.block().length()
        var previousInst = pos
        for (i in pos until pos + len) {
            try {
                if (iterator.byteAt(i) in invokeOpcodes) {
                    val invokedMethod = iterator.u16bitAt(i + 1)
                    val c = constPool.getMethodrefClassName(invokedMethod)
                    val m = constPool.getMethodrefName(invokedMethod)
                    val d = constPool.getMethodrefType(invokedMethod)
                    val clazz = classPool.get(c)
                    val method = if (m in initsName)
                        clazz.getConstructor(d).toMethod(m, classPool.get(c))
                    else
                        clazz.getMethod(m, d)
                    if (Modifier.isNative(method.modifiers))
                        continue
                    val methodAnalyzer = MethodAnalyzer(method)
                    val possibleExceptions = methodAnalyzer.getPossibleExceptions()
                    if (possibleExceptions.allExceptions.isNotEmpty())
                        return false
                }
                if (iterator.byteAt(i) == Opcode.ATHROW) {
                    val exception = cfg.frameAt(if (isKotlin) previousInst else i).getStack(0)
                    return if (exception.toString() == "java.lang.Throwable") {
                        hasEmptyFinally = true
                        true
                    } else {
                        false
                    }
                } else if (isKotlin && cfg.frameAt(i) != null) {
                    previousInst = i
                }
            } catch (e: Exception) {
                continue
            }
        }
        return false
    }
}