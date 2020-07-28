package org.jetbrains.research.jem

import javassist.CtMethod
import javassist.NotFoundException
import javassist.bytecode.BadBytecode
import javassist.bytecode.analysis.*
import java.util.concurrent.ConcurrentHashMap

class MethodAnalyzer(private val method: CtMethod) {

    init {
        if (!previousMethods.containsKey(InvokeAnalyzer.exactMethodName(method))) {
            try {
                previousMethods[InvokeAnalyzer.exactMethodName(method)] =
                        method.exceptionTypes.map { it.name }.toSet()
            } catch (e: NotFoundException) {}
        }
    }

    companion object {
        val previousMethods = ConcurrentHashMap<String, Set<String>>()
    }

    fun getPossibleExceptions(): Set<String> {
        val cfg = try {
            ControlFlow(method)
        } catch (e: BadBytecode) {
            return setOf()
        }
        val tree = cfg.dominatorTree() ?:
            return previousMethods[InvokeAnalyzer.exactMethodName(method)] ?:
                setOf()
        val catchBlocks = getAllCatchBlocks(tree)
        val blockAnalyzer = BlockAnalyzer(method)
        val invokeAnalyzer = InvokeAnalyzer(method, blockAnalyzer)
        val exceptions = mutableSetOf<String>()
        for (node in tree) {
            if (isReachable(node, catchBlocks, blockAnalyzer)) {
                exceptions.addAll(blockAnalyzer
                        .getThrownExceptions(node.block()))
                exceptions.addAll(invokeAnalyzer
                        .getPossibleExceptionsFromMethods(node.block()))
            }
        }
        if (blockAnalyzer.hasEmptyFinally) {
            exceptions.remove("java.lang.Throwable")
        }
        if (exceptions.isNotEmpty()) {
            previousMethods[InvokeAnalyzer.exactMethodName(method)] =
                    exceptions
        }
       return exceptions
    }

    private fun isReachable(node: ControlFlow.Node,
                            catchBlocks: Set<ControlFlow.Block>,
                            blockAnalyzer: BlockAnalyzer): Boolean =
        if (node.block() in catchBlocks)
            node.block() in blockAnalyzer.reachableCatchBlocks
        else
            true

    private fun getAllCatchBlocks(tree: Array<ControlFlow.Node>): Set<ControlFlow.Block> =
            tree.map{
                it.block().catchers().map { c -> c.block() }
            }.flatten().toSet()
}