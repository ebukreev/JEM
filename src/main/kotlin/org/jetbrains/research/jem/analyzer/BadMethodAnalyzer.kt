package org.jetbrains.research.jem.analyzer

import org.jetbrains.research.kfg.Package
import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.CatchBlock
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.ir.value.Value
import org.jetbrains.research.kfg.ir.value.instruction.NewInst
import org.jetbrains.research.kfg.ir.value.instruction.ThrowInst
import org.jetbrains.research.kfg.type.Type
import org.objectweb.asm.commons.TryCatchBlockSorter
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import java.lang.Class.forName

class BadMethodAnalyzer(methodForAnalysis: Method) {

    private val method = methodForAnalysis
    private val catchesSuccessors = getCatchesSuccessors(method)
    private val emptyFinallyBlocks = method.catchEntries.filter { isEmptyFinallyBlock(it) }

    fun getThrownExceptions(): Set<String> {
        val exceptions = mutableSetOf<String>()
        for (block in method.basicBlocks) {
            if (block !in catchesSuccessors) {
                block.instructions.forEach {
                    if (it is ThrowInst &&
                            !isAnyCaught(method.bodyBlocks, it.throwable.type)) {
                        exceptions.add(it.throwable.type
                                .toString().replace("/", "."))
                    }
                }
            }
        }
        exceptions.addAll(getExceptionsFromCatchBlocks(method))
        //method.mn.tryCatchBlocks.forEach { println((it as TryCatchBlockNode).type);println((it as TryCatchBlockNode));println((it as TryCatchBlockNode).end) ;println("@@@@"); println("------------------------") }
        if (method.catchEntries.any { b -> isEmptyFinallyBlock(b) }) {
            exceptions.remove("java.lang.Throwable")
        }
        return exceptions

    }

    private fun getReachableCatches(method: Method): HashSet<CatchBlock> {
        val result = mutableSetOf<CatchBlock>()
        for (block in method.catchEntries) {
            val possibleExceptions = mutableSetOf<Type>()
            block.throwers.forEach { t -> getAllSuccessors(t)
                    .forEach {b ->
                        b.instructions.forEach {
                            if (it is ThrowInst)
                                possibleExceptions.add(it.throwable.type)
                        }
                    }
            }
            if (possibleExceptions.any { block.exception.isSupertypeOf(it) }) {
                result.add(block)
            }
        }
        return result.toHashSet()
    }

    private fun getCatchesSuccessors(method: Method): Set<BasicBlock> =
            method.catchEntries.map { getAllSuccessors(it) }.flatten().toSet()


    private fun getExceptionsFromCatchBlocks(method: Method): Set<String> {
        val exceptions = mutableSetOf<String>()
        for (block in getReachableCatches(method)) {
            val successors = getAllSuccessors(block)
            successors.forEach { b ->
                b.instructions.forEach {
                    if (it is ThrowInst && !isAnyCaught(successors, it.throwable.type) ) {
                        exceptions.add(it.throwable.type
                            .toString().replace("/", "."))
                    }
                }
            }
        }
        return exceptions
    }

    private fun getAllSuccessors(block: BasicBlock): List<BasicBlock> {
        val result = mutableListOf(block)
        block.successors.forEach { result += getAllSuccessors(it) }
        return result
    }

    private fun isAnyCaught(blocks: Collection<BasicBlock>, exception: Type) =
            blocks.any { isCaught(it, exception) }

    private fun isCaught(block: BasicBlock, exception: Type): Boolean {
        for (h in block.handlers) {
            if (exceptionIsSupertypeOf(h.exception, exception) &&
                    h !in emptyFinallyBlocks) {
                return true
            }
        }
        return false
    }

    private fun exceptionIsSupertypeOf(firstException: Type, secondException: Type): Boolean {
        try {
            return forName(firstException.toString().replace("/", "."))
                    .isAssignableFrom(forName(secondException.toString().replace("/", ".")))
        } catch (e: ClassNotFoundException) {}
        return firstException.isSupertypeOf(secondException)
    }

    private fun isEmptyFinallyBlock(handler: CatchBlock): Boolean {
        return handler.exception.toString() == "java/lang/Throwable" &&
                hasThrowThrowable(handler)
    }

    private fun hasThrowThrowable(handler: BasicBlock): Boolean {
        if (handler.successors.isEmpty()) {
            return handler.instructions
                    .any {
                        it is ThrowInst &&
                                it.throwable.type.toString() == "java/lang/Throwable"
                    }
        } else {
            for (child in handler.successors) {
                if (hasThrowThrowable(child)) {
                    return true
                }
            }
        }
        return false
    }
}