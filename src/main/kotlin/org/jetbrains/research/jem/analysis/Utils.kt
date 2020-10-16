package org.jetbrains.research.jem.analysis

import javassist.NotFoundException
import org.jetbrains.research.jem.interaction.ExceptionsAndCalls
import org.jetbrains.research.jem.interaction.MethodInformation

//fun ExceptionsAndCalls.toAllExceptions(): Set<String> {
//    val result = exceptions
//    if (this.calls.isEmpty())
//        return result
//    for ((call, caught) in calls) {
//        try {
//            result.addAll(
//                MethodAnalyzer.polyMethodsExceptions[
//                        MethodInformation(
//                            MethodAnalyzer.classPool
//                                .getCtClass(call.clazz)
//                                .getMethod(call.name, call.descriptor)
//                        )
//                ]?.minus(caught) ?: MethodAnalyzer.previousMethods[
//                        MethodInformation(
//                            MethodAnalyzer.classPool
//                                .getCtClass(call.clazz)
//                                .getMethod(call.name, call.descriptor)
//                        )
//                ]?.toAllExceptions()
//                    ?.minus(caught)
//                ?: emptySet()
//            )
//        } catch (e: NotFoundException) {
//            continue
//        }
//    }
//    return result
//}