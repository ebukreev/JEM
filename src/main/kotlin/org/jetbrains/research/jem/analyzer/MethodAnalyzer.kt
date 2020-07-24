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


    fun getPossibleExceptions(): Set<String> {
        val result = mutableSetOf<String>()
        for (node in tree) {
            if (node.children() == 0 && isReachable(node)) {
                result.addAll(getThrownExceptions(node.block()))
            }
        }
        return result.minus("java.lang.Throwable")
    }
    /*{
        val exceptions = mutableSetOf<String>()
        for (node in tree) {
            if (node.block() !in catchBlocks) {
                val thrownExceptions = getThrownExceptions(node.block())
                exceptions.addAll(thrownExceptions)
            }
        }
        for (block in catchBlocks) {
            if (block.clientData == "catch") {
                val thrownExceptions = getThrownExceptions(block)
                exceptions.addAll(thrownExceptions)
            }
        }
        return exceptions
    }*/

    private fun isReachable(node: ControlFlow.Node): Boolean {
        if (node.block() in catchBlocks) {
            return node.block().clientData == "catch"
        }
        return true
    }

    private fun getThrownExceptions(block: ControlFlow.Block): Set<String> {
        val result = mutableSetOf<String>()
        val pos = block.position()
        val len = block.length()
        for (i in pos until pos + len) {
            val iter = method.methodInfo2.codeAttribute.iterator()
            iter.move(i)
            if (iter.byteAt(i) == Opcode.ATHROW) {
                val exception = cfg.frameAt(i).getStack(0)
                if (!isCaught(block, exception)) {
                    result.add(exception.toString())
                }
            }
        }
        return result
    }

    private fun isCaught(block: ControlFlow.Block, exception: Type): Boolean {
        for (catcher in block.catchers()) {
            if (classPool.get(exception.toString())
                            .subclassOf(classPool.get(catcher.type())) &&
                    !isEmptyFinallyBlock(catcher) && catcher.block() != block) {
                catcher.block().clientData = "catch"
                return true
            }
        }
        return false
    }

    private fun isEmptyFinallyBlock(catcher: ControlFlow.Catcher): Boolean {
        return catcher.type() == "java.lang.Throwable" && hasThrowThrowable(catcher)
    }

    private fun hasThrowThrowable(catcher: ControlFlow.Catcher): Boolean {
        for (i in catcher.block().position() until catcher.block().position() + catcher.block().length()) {
            val iter = method.methodInfo2.codeAttribute.iterator()
            iter.move(i)
            if (iter.byteAt(i) == Opcode.ATHROW) {
                val exception = cfg.frameAt(i).getStack(0)
                return exception.toString() == "java.lang.Throwable"
            }
        }
        return false
    }

    private fun getAllCatchBlocks(): Set<ControlFlow.Block> {
        val result = mutableSetOf<ControlFlow.Block>()
        tree.forEach { result.addAll(it.block().catchers().map { c -> c.block() }) }
        return result
    }
}

fun main() {
    val pool = ClassPool.getDefault()
    pool.insertClassPath("./demo")
    val cc = pool.get("Main")
    val method = cc.getDeclaredMethod("test3")
    val analyser = MethodAnalyzer(method)
    println(analyser.getPossibleExceptions())
}