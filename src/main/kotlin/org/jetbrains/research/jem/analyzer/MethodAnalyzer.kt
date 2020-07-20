package org.jetbrains.research.jem.analyzer

import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.ir.value.instruction.ThrowInst
import org.jetbrains.research.kfg.type.Type

fun getThrownExceptions(method: Method): Set<String> {
    val exceptions = mutableSetOf<String>()
    method.basicBlocks
            .forEach { b -> b.instructions
                    .forEach {
                        if (it is ThrowInst && !isCaught(b, it.throwable.type)) {
                            exceptions.add(it.throwable.type
                                    .toString().replace("/", "."))
                        }
                    }
            }
    return exceptions
}

fun isCaught(block: BasicBlock, exception: Type): Boolean {
    for (h in block.handlers) {
        if (h.exception.isSupertypeOf(exception)) {
            return true
        }
    }
    return false
}