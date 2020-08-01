
import javassist.ClassPool
import org.jetbrains.research.jem.MethodAnalyzer
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {

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
        val pool = ClassPool.getDefault()
        pool.insertClassPath("./src/test/resources")
        pool.insertClassPath("./src/test/resources/kotlin")
        val `class` = pool.get("TryCatchFinallyTest")
        for (i in 1..7) {
            val method = `class`.getDeclaredMethod("test$i")
            val ma = MethodAnalyzer(method)
            println(i)
            assertEquals(ma.getPossibleExceptions(), exceptions[i - 1])
        }
        val kclass = pool.get("KotlinTryCatchFinallyTest")
        for (i in 1..7) {
            val method = kclass.getDeclaredMethod("test$i")
            val ma = MethodAnalyzer(method)
            println(i)
            assertEquals(ma.getPossibleExceptions(), exceptions[i - 1])
        }
    }
}
