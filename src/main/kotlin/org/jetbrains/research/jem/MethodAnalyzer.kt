package org.jetbrains.research.jem

import javassist.CtMethod
import javassist.NotFoundException
import javassist.bytecode.BadBytecode
import javassist.bytecode.analysis.*
import java.util.concurrent.ConcurrentHashMap

class MethodAnalyzer(private val method: CtMethod) {

    private val methodInformation = MethodInformation(method)

    init {
        try {
            previousMethods[methodInformation] =
                    method.exceptionTypes.map { it.name }.toSet()
        } catch (e: NotFoundException) {}
    }

    internal companion object {
        var polyMethodsExceptions = PolymorphismAnalyzer(emptyArray()).methodToExceptions
        fun initPolymorphismAnalyzer(pa: PolymorphismAnalyzer) {
            polyMethodsExceptions = pa.methodToExceptions
        }
        val previousMethods = ConcurrentHashMap<MethodInformation, Set<String>>()
    }

    fun getPossibleExceptions(): Set<String> {
        val cfg = try {
            ControlFlow(method)
        } catch (e: BadBytecode) {
            return setOf()
        }
        val tree = cfg.dominatorTree() ?:
            return previousMethods.getValue(methodInformation)
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
            previousMethods[methodInformation] =
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