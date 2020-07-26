/*import java.io.IOException
import java.util.*

class KotlinTryCatchFinallyTest {
    fun test1() {
        try {
            throw IOException()
        } catch (e: IOException) {
            throw IllegalArgumentException()
        }
    }

    fun test2() {
        throw IllegalAccessException()
    }

    fun test3() {
        val e = IllegalAccessError()
        try {
            throw e
        } catch (p: NullPointerException) {
            throw IndexOutOfBoundsException()
        } finally {
        }
    }

    fun test4() {
        val e = IllegalAccessError()
        try {
            throw e
        } catch (w: Exception) {
            throw IndexOutOfBoundsException()
        } finally {
            throw IOException()
        }
    }

    fun test5() {
        val f = IOException()
        val g = IndexOutOfBoundsException()
        try {
            try {
                try {
                    throw f
                } catch (h: IOException) {
                } finally {
                    throw g
                }
            } catch (t: IndexOutOfBoundsException) {
                try {
                    throw Exception()
                } catch (y: Exception) {
                    if (Random().nextBoolean()) {
                        throw InterruptedException()
                    } else {
                        throw RuntimeException()
                    }
                }
            }
        } catch (s: InterruptedException) {
            throw StackOverflowError()
        } catch (a: RuntimeException) {
        } finally {
        }
    }

    fun test6() {
        if (Random().nextBoolean()) {
            throw IllegalAccessException()
        } else {
            throw IOException()
        }
    }

    fun test7() {
        try {
            if (Random().nextBoolean()) {
                throw IllegalAccessException()
            } else {
                throw IOException()
            }
        } catch (e: IOException) {
        } finally {
        }
    }

    fun test8() {
        try {
            println("hi")
            throw KotlinMyException()
        } catch (p: Error) {
            throw IndexOutOfBoundsException()
        } finally {
        }
    }
}

class KotlinMyException : RuntimeException()

 */