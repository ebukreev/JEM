package org.jetbrains.research.jem

import javassist.CtMethod
import javassist.bytecode.analysis.*

class MethodAnalyzer(methodForAnalysis: CtMethod) {

    private val method = methodForAnalysis
    private val cfg = ControlFlow(method)
    private val tree = cfg.dominatorTree()
    private val catchBlocks = getAllCatchBlocks()
    private val blockAnalyzer = BlockAnalyzer(method)
    private val invokeAnalyzer = InvokeAnalyzer(method, blockAnalyzer)

    fun getPossibleExceptions(): Set<String> {
        val exceptions = mutableSetOf<String>()
        for (node in tree) {
            if (isReachable(node)) {
                exceptions.addAll(blockAnalyzer
                        .getThrownExceptions(node.block()))
                exceptions.addAll(invokeAnalyzer
                        .getPossibleExceptionsFromMethods(node.block()))
            }
        }
        return if (blockAnalyzer.hasEmptyFinally)
            exceptions.minus("java.lang.Throwable")
        else
            exceptions
    }

    private fun isReachable(node: ControlFlow.Node): Boolean =
        if (node.block() in catchBlocks)
            node.block() in blockAnalyzer.reachableCatchBlocks
        else
            true

    private fun getAllCatchBlocks(): Set<ControlFlow.Block> =
            tree.map{
                it.block().catchers().map { c -> c.block() }
            }.flatten().toSet()
}