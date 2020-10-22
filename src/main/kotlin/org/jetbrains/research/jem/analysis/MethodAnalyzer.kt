package org.jetbrains.research.jem.analysis

import javassist.ClassPool
import javassist.CtBehavior
import javassist.NotFoundException
import javassist.bytecode.BadBytecode
import javassist.bytecode.analysis.*
import org.jetbrains.research.jem.interaction.ExceptionsAndCalls
import org.jetbrains.research.jem.interaction.MethodInformation
import java.util.concurrent.ConcurrentHashMap

class MethodAnalyzer(private val method: CtBehavior) {

    private val methodInformation = MethodInformation(method)

    init {
        try {
            previousMethods[methodInformation] =
                    ExceptionsAndCalls.empty()
        } catch (e: NotFoundException) {}
    }

    internal companion object {
        lateinit var polyMethodsExceptions: MutableMap<MethodInformation, Set<String>>
        fun polyMethodsExceptionsIsInitialized(): Boolean =
            ::polyMethodsExceptions.isInitialized

        val previousMethods = ConcurrentHashMap<MethodInformation, ExceptionsAndCalls>()
        val classPool: ClassPool = ClassPool.getDefault()
    }

    fun getPossibleExceptions(): ExceptionsAndCalls {
        if (polyMethodsExceptionsIsInitialized()
            && polyMethodsExceptions.containsKey(methodInformation)) {
            return ExceptionsAndCalls(
                polyMethodsExceptions
                    .getValue(methodInformation) as MutableSet<String>, 
                mutableMapOf(),
                polyMethodsExceptions
                    .getValue(methodInformation) as MutableSet<String>
            )
        }
        val cfg = try {
            ControlFlow(method.declaringClass, method.methodInfo2)
        } catch (e: BadBytecode) {
            return ExceptionsAndCalls.empty()
        }
        val tree = cfg.dominatorTree() ?:
            return previousMethods.getValue(methodInformation)
        val catchBlocks = getAllCatchBlocks(tree)
        val blockAnalyzer = BlockAnalyzer(method)
        val invokeAnalyzer =
            InvokeAnalyzer(method, blockAnalyzer)
        var exceptionsAndCalls = ExceptionsAndCalls.empty()
        for (node in tree) {
            if (isReachable(node, catchBlocks, blockAnalyzer)) {
                val thrownExceptions =
                    blockAnalyzer.getThrownExceptions(node.block())
                val callsWithCaughtAndExceptions =
                    invokeAnalyzer.getCallsWithCaughtAndExceptions(node.block())
                exceptionsAndCalls.exceptions.addAll(thrownExceptions)
                exceptionsAndCalls.calls.putAll(callsWithCaughtAndExceptions.first)
                exceptionsAndCalls.allExceptions.addAll(
                    thrownExceptions + callsWithCaughtAndExceptions.second
                )
            }
        }
        if (blockAnalyzer.hasEmptyFinally) {
            exceptionsAndCalls.exceptions.remove("java.lang.Throwable")
            exceptionsAndCalls.allExceptions.remove("java.lang.Throwable")
        }
        while (exceptionsAndCalls.allExceptions !=
            previousMethods.getValue(methodInformation).allExceptions
        ) {
            previousMethods[methodInformation] =
                exceptionsAndCalls
            exceptionsAndCalls = getPossibleExceptions()
        }
        return exceptionsAndCalls
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