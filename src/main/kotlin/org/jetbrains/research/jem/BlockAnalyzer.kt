package org.jetbrains.research.jem

import javassist.CtMethod
import javassist.bytecode.Opcode
import javassist.bytecode.analysis.ControlFlow

internal class BlockAnalyzer(method: CtMethod) {

    private val iterator = method.methodInfo2.codeAttribute.iterator()
    private val cfg = ControlFlow(method)
    private val classPool = method.declaringClass.classPool
    internal val reachableCatchBlocks = mutableSetOf<ControlFlow.Block>()
    private val invokeAnalyzer = InvokeAnalyzer(method, this)
    internal var hasEmptyFinally = false

    fun getThrownExceptions(block: ControlFlow.Block): Set<String> {
        val exceptions = mutableSetOf<String>()
        val pos = block.position()
        val len = block.length()
        for (i in pos until pos + len) {
            if (iterator.byteAt(i) == Opcode.ATHROW) {
                val exception = cfg.frameAt(i).getStack(0)
                if (!isCaught(block, exception.toString())) {
                    exceptions.add(exception.toString())
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
                    hasThrowThrowable(catcher) &&
                    invokeAnalyzer
                            .getPossibleExceptionsFromMethods(catcher.block()).isEmpty()

    private fun hasThrowThrowable(catcher: ControlFlow.Catcher): Boolean {
        val pos = catcher.block().position()
        val len = catcher.block().length()
        for (i in  pos until pos + len) {
            if (iterator.byteAt(i) == Opcode.ATHROW) {
                val exception = cfg.frameAt(i).getStack(0)
                return if (exception.toString() == "java.lang.Throwable") {
                    hasEmptyFinally = true
                    true
                } else {
                    false
                }
            }
        }
        return false
    }
}