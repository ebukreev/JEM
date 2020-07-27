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

    init {
        if (!previousMethods.containsKey(invokeAnalyzer.exactMethodName(method)))
            previousMethods[invokeAnalyzer.exactMethodName(method)] =
                    method.exceptionTypes.map { it.name }.toSet()

    }

    companion object {
        val previousMethods = hashMapOf<String, Set<String>>()
    }

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
        if (blockAnalyzer.hasEmptyFinally) {
            exceptions.remove("java.lang.Throwable")
        }
        previousMethods[invokeAnalyzer.exactMethodName(method)] =
                exceptions
       return exceptions
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