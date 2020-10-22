package org.jetbrains.research.jem.interaction

import kotlinx.serialization.json.Json
import java.io.FileReader
import kotlinx.serialization.*

object InfoReader {

    private fun read(pathToJson: String): Package {
        val json = Json { isLenient = true }
        FileReader(pathToJson).use {
            return json.decodeFromString(it.readText())
        }
    }

    fun getAllExceptionsFor(method: MethodInformation): Set<String> {
        val graph = try {
             buildGraph(method)
        } catch (e: NullPointerException) {
            return emptySet()
        }
        val result = mutableSetOf<String>()
        for (exc in graph.getExceptionsVertices()) {
            val paths = mutableSetOf<String>()
            graph.getAllPaths(graph[method.toString()]!!, exc, "", paths)
            result.addAll(paths)
        }
        return result
    }

    private val packs = mutableMapOf<String, Package>()
    private val methodInf = mutableMapOf<String, ExceptionsAndCalls>()

    fun getExceptionsInfoFor(call: String): ExceptionsAndCalls {
        if (methodInf.containsKey(call))
            return methodInf.getValue(call)
        val methodInfo = call.toMethodInfo()
        val jsonPath = System.getProperty("user.home") +
                "/.JEMPluginCache/" +
                methodInfo.clazz!!
                    .replace("(\\.[A-Z].*)".toRegex(), "")
                    .replace(".", "/") +
                "/${methodInfo.clazz.replace("(\\.[A-Z].*)".toRegex(), "")}.json"
        val packOfThisCall = if (packs.containsKey(jsonPath)) {
            packs.getValue(jsonPath)
        } else {
            val pack = read(jsonPath)
            packs[jsonPath] = pack
            pack
        }
        val result = packOfThisCall.classes
            .find { it.className == methodInfo.clazz }
            ?.methods
            ?.find { it.methodName == methodInfo.name && it.descriptor == methodInfo.descriptor }
            ?.exceptionsInfo!!
        methodInf[call] = result
        return result
    }
}