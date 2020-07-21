package org.jetbrains.research.jem.analyzer

import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.CatchBlock
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.ir.value.instruction.ThrowInst
import org.jetbrains.research.kfg.type.Type

object MethodAnalyzer {
    fun getThrownExceptions(method: Method): Set<String> {
        val exceptions = mutableSetOf<String>()
        method.basicBlocks
                .forEach { b ->
                    b.instructions
                            .forEach {
                                if (it is ThrowInst && !isCaught(b, it.throwable.type)) {
                                    exceptions.add(it.throwable.type
                                            .toString().replace("/", "."))
                                }
                            }
                }
        if (method.catchEntries.any { b -> isEmptyFinallyBlock(b) }) {
            exceptions.remove("java.lang.Throwable")
        }
        return exceptions
    }

    private fun isCaught(block: BasicBlock, exception: Type): Boolean {
        for (h in block.handlers) {
            if (h.exception.isSupertypeOf(exception) && !isEmptyFinallyBlock(h)) {
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