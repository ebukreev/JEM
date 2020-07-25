package org.jetbrains.research.jem

import javassist.CtMethod
import javassist.bytecode.Opcode
import javassist.bytecode.analysis.*

class MethodAnalyzer(methodForAnalysis: CtMethod) {

    private val method = methodForAnalysis
    private val cfg = ControlFlow(method)
    private val tree = cfg.dominatorTree()
    private val catchBlocks = getAllCatchBlocks()
    private val classPool = method.declaringClass.classPool
    private val invokeAnalyzer = InvokeAnalyzer(method)
    private val blockAnalyzer = BlockAnalyzer(method)
    private val initsName = setOf("<init>", "<clinit>")

    fun getPossibleExceptions(): Set<String> {
        val exceptions = mutableSetOf<String>()
        for (node in tree) {
            if (node.children() == 0 && isReachable(node)) {
                exceptions.addAll(blockAnalyzer.getThrownExceptions(node.block()))
                for ((c, m, d) in invokeAnalyzer.getInvokedMethods(node.block())) {
                    val `class` = classPool.get(c)
                    val method = if (m in initsName)
                        `class`.getConstructor(d).toMethod(m, classPool.get(c))
                    else
                        `class`.getMethod(m, d)
                    val methodAnalyzer = MethodAnalyzer(method)
                    val possibleExceptions = methodAnalyzer
                            .getPossibleExceptions()
                            .takeWhile { !blockAnalyzer.isCaught(node.block(), it) }
                    exceptions.addAll(possibleExceptions)
                }
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