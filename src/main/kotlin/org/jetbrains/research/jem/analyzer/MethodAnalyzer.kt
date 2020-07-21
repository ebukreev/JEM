package org.jetbrains.research.jem.analyzer

import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.CatchBlock
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.ir.value.instruction.ThrowInst
import org.jetbrains.research.kfg.type.Type

object MethodAnalyzer {
    fun getThrownExceptions(method: Method): Set<String> {
        val exceptions = mutableSetOf<String>()
        for (block in method.basicBlocks) {
            val catchesSuccessors = getCatchesSuccessors(method)
            if (block !in catchesSuccessors) {
                block.instructions
                        .forEach {
                            if (it is ThrowInst && !isCaught(block, it.throwable.type)) {
                                exceptions.add(it.throwable.type
                                        .toString().replace("/", "."))
                            }
                        }
            }
        }
        exceptions.addAll(getExceptionsFromCatchBlocks(method))
        if (method.catchEntries.any { b -> isEmptyFinallyBlock(b) }) {
            exceptions.remove("java.lang.Throwable")
        }
        return exceptions
    }

    private fun getCatchesSuccessors(method: Method): Set<BasicBlock> {
        val result = mutableSetOf<BasicBlock>()
        method.catchEntries.forEach { result.addAll(getAllSuccessors(it)) }
        return result
    }

    private fun getExceptionsFromCatchBlocks(method: Method): Set<String> {
        val exceptions = mutableSetOf<String>()
        for (block in method.catchEntries) {
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
        for (child in block.successors) {
            result += getAllSuccessors(child)
        }
        return result
    }

    private fun isAnyCaught(blocks: Collection<BasicBlock>, exception: Type) =
            blocks.any { isCaught(it, exception) }

    private fun isCaught(block: BasicBlock, exception: Type): Boolean {
        for (h in block.handlers) {
            if (h.exception.isSupertypeOf(exception) &&
                    !isEmptyFinallyBlock(h)) {
                return true
            }
        }
        return false
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