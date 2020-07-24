package org.jetbrains.research.jem.analyzer

import javassist.ClassPool
import javassist.CtMethod
import javassist.bytecode.Opcode
import javassist.bytecode.analysis.*

class MethodAnalyzer(methodForAnalysis: CtMethod) {

    private val method = methodForAnalysis
    private val cfg = ControlFlow(method)
    private val tree = cfg.dominatorTree()
    private val catchBlocks = getAllCatchBlocks()
    private val classPool = method.declaringClass.classPool
    private val reachableCatchBlocks = mutableSetOf<ControlFlow.Block>()
    private val iterator = method.methodInfo2.codeAttribute.iterator()
    private var hasEmptyFinally = false

    fun getPossibleExceptions(): Set<String> {
        val exceptions = mutableSetOf<String>()
        for (node in tree) {
            if (node.children() == 0 && isReachable(node)) {
                exceptions.addAll(getThrownExceptions(node.block()))
            }
        }
        return if (hasEmptyFinally)
            exceptions.minus("java.lang.Throwable")
        else
            exceptions
    }

    private fun isReachable(node: ControlFlow.Node): Boolean =
        if (node.block() in catchBlocks)
            node.block() in reachableCatchBlocks
        else
            true

    private fun getThrownExceptions(block: ControlFlow.Block): Set<String> {
        val exceptions = mutableSetOf<String>()
        val pos = block.position()
        val len = block.length()
        for (i in pos until pos + len) {
            if (iterator.byteAt(i) == Opcode.ATHROW) {
                val exception = cfg.frameAt(i).getStack(0)
                if (!isCaught(block, exception)) {
                    exceptions.add(exception.toString())
                }
            }
        }
        return exceptions
    }

    private fun isCaught(block: ControlFlow.Block, exception: Type): Boolean {
        block.catchers().forEach {
            if (isSubclass(exception.toString(), it.type()) &&
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

    private fun getAllCatchBlocks(): Set<ControlFlow.Block> =
            tree.map{
                it.block().catchers().map { c -> c.block() }
            }.flatten().toSet()
}