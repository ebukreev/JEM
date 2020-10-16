package org.jetbrains.research.jem.test
import javassist.ClassPool
import org.jetbrains.research.jem.analysis.MethodAnalyzer
import org.jetbrains.research.jem.analysis.PolymorphismAnalyzer
import org.junit.Test
import kotlin.test.assertEquals

class Tests {

    private val pool: ClassPool = ClassPool.getDefault().apply {
        insertClassPath("./src/test/resources/compiled")
    }

    @Test
    fun tryCatchFinallyTest() {
        val exceptions = listOf(
                setOf("java.lang.IllegalArgumentException"),
                setOf("java.lang.IllegalAccessException"),
                setOf("java.lang.IllegalAccessError"),
                setOf("java.io.IOException"),
                setOf("java.lang.StackOverflowError"),
                setOf("java.lang.IllegalAccessException", "java.io.IOException"),
                setOf("java.lang.IllegalAccessException"),
                setOf("MyException", "java.lang.IndexOutOfBoundsException"),
                setOf("KotlinMyException", "java.lang.IndexOutOfBoundsException")
        )
        val `class` = pool.get("org.jetbrains.research.jem.test.testcode.TryCatchFinallyTest")
        for (i in 1..7) {
            val method = `class`.getDeclaredMethod("test$i")
            val ma = MethodAnalyzer(method)
            println(i)
            assertEquals(ma.getPossibleExceptions().allExceptions, exceptions[i - 1])
        }
        val kclass = pool.get("org.jetbrains.research.jem.test.testcode.KotlinTryCatchFinallyTest")
        for (i in 1..7) {
            val method = kclass.getDeclaredMethod("test$i")
            val ma = MethodAnalyzer(method)
            println(i)
            assertEquals(ma.getPossibleExceptions().allExceptions, exceptions[i - 1])
        }
    }

    @Test
    fun polyTest() {
        val exceptions = listOf(
            setOf("java.lang.IllegalArgumentException"),
            setOf("java.lang.IllegalArgumentException"),
            emptySet(),
            setOf("java.lang.IllegalArgumentException"),
            setOf(),
            setOf("java.lang.IllegalStateException"),
            setOf("java.lang.IllegalStateException", "java.lang.IllegalArgumentException"),
            setOf("java.lang.IllegalStateException"),
            emptySet(),
            emptySet(),
            emptySet()
        )
        val classes = pool.get(
            List(10) { "org.jetbrains.research.jem.test.testcode.Test${it + 1}" }.toTypedArray()
        )
        MethodAnalyzer.polyMethodsExceptions =
            PolymorphismAnalyzer(classes).methodToExceptions.toMutableMap()
        for (i in 1..11) {
            val clazz = pool.get("org.jetbrains.research.jem.test.testcode.Test$i")
            val method = clazz.getDeclaredMethod("foo")
            val ma = MethodAnalyzer(method)
            println(i)
            assertEquals(ma.getPossibleExceptions().allExceptions, exceptions[i - 1])
        }
    }

    @Test
    fun recursionTest() {
        val clazz = pool.get("org.jetbrains.research.jem.test.testcode.RecursionTestKt")
        for (m in clazz.methods) {
            println(m.name)
            println(MethodAnalyzer(m).getPossibleExceptions())
        }
    }
}
