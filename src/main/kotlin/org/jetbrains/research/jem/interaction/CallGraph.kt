package org.jetbrains.research.jem.interaction

import java.io.FileNotFoundException

class Graph {
    data class Vertex(val name: String, val isException: Boolean, val view: String) {
        val neighbors = mutableSetOf<Vertex>()
    }

    private val vertices = mutableMapOf<String, Vertex>()

    operator fun get(name: String) = vertices[name]

    fun getExceptionsVertices() = vertices.values.filter { it.isException }

    fun addVertex(name: String, isException: Boolean, view: String) {
        vertices[name] = Vertex(name, isException, view)
    }

    private fun connect(first: Vertex, second: Vertex) {
        first.neighbors.add(second)
    }

    fun connect(first: String, second: String) = connect(this[first]!!, this[second]!!)

    fun getAllPaths(
        start: Vertex,
        end: Vertex,
        prevPath: String,
        paths: MutableSet<String>,
        visited: MutableSet<Vertex> = mutableSetOf(),
    ) {
        if (start == end) {
            paths.add(prevPath + end.view)
            return
        }
        visited.add(start)
        start.neighbors
            .filter { it !in visited }
            .forEach { getAllPaths(it, end, prevPath + start.view, paths, visited) }
        visited.remove(start)
    }
}

fun buildGraph(method: MethodInformation) =
    Graph().apply {
        addVertex(
            method.toString(),
            false,
            "${method.clazz} ${method.name}"
        )
        buildGraph(method, emptySet(), this)
    }

private fun buildGraph(methodCall: MethodInformation, caught: Set<String>, g: Graph) {
    val methodCallAsString = methodCall.toString()
    val info = InfoReader.getExceptionsInfoFor(
        if (methodCall.name == "<init>")
            methodCall.clazz
                    + " "
                    + methodCall.clazz!!.substringAfterLast(".")
                    + " "
                    + methodCall.descriptor
        else
            methodCallAsString
    )
    info.exceptions.forEach {
        if (it !in caught) {
            g.addVertex(it, true, " : $it")
            g.connect(methodCallAsString, it)
        }
    }
    info.calls.forEach { (call, caught) ->
        if (g[call] != null) {
            g.connect(methodCallAsString, call)
        } else {
            g.addVertex(call, false, " -> ${call.toMethodInfo().clazz} ${call.toMethodInfo().name}")
            g.connect(methodCallAsString, call)
            try {
                buildGraph(call.toMethodInfo(), caught, g)
            } catch (e: NullPointerException) {
                info.allExceptions.forEach {
                    if (it !in caught) {
                        g.addVertex(it, true, " : $it")
                        g.connect(methodCallAsString, it)
                    }
                }
                //TODO что то с этим надо сделать
            } catch (e: FileNotFoundException) {
                info.allExceptions.forEach {
                    if (it !in caught) {
                        g.addVertex(it, true, " : $it")
                        g.connect(methodCallAsString, it)
                    }
                }
            }
        }
    }
}