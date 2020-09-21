package org.jetbrains.research.jem.analysis

import javassist.CtMethod
import javassist.NotFoundException
import javassist.bytecode.BadBytecode
import javassist.bytecode.analysis.*
import org.jetbrains.research.jem.interaction.MethodInformation
import java.util.concurrent.ConcurrentHashMap

class MethodAnalyzer(private val method: CtMethod) {

    private val methodInformation = MethodInformation(method)

    init {
        try {
            previousMethods[methodInformation] =
                    emptySet()
        } catch (e: NotFoundException) {}
    }

    internal companion object {
        lateinit var polyMethodsExceptions: Map<MethodInformation, Set<String>>
        fun polyMethodsExceptionsIsInitialized(): Boolean =
            ::polyMethodsExceptions.isInitialized

        val previousMethods = ConcurrentHashMap<MethodInformation, Set<String>>()
    }

    fun getPossibleExceptions(): Set<String> {
        if (polyMethodsExceptionsIsInitialized()
            && polyMethodsExceptions.containsKey(methodInformation)) {
            return polyMethodsExceptions.getValue(methodInformation)
        }
        val cfg = try {
            ControlFlow(method)
        } catch (e: BadBytecode) {
            return setOf()
        }
        val tree = cfg.dominatorTree() ?:
            return previousMethods.getValue(methodInformation)
        val catchBlocks = getAllCatchBlocks(tree)
        val blockAnalyzer = BlockAnalyzer(method)
        val invokeAnalyzer =
            InvokeAnalyzer(method, blockAnalyzer)
        var exceptions = mutableSetOf<String>()
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
        while (exceptions != previousMethods.getValue(methodInformation)) {
            previousMethods[methodInformation] =
                    exceptions
            exceptions = getPossibleExceptions() as MutableSet<String>
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