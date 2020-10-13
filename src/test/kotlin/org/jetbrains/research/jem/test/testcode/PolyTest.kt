@file:Suppress("unused")
package org.jetbrains.research.jem.test.testcode
//
//import java.lang.IllegalArgumentException
//import kotlin.random.Random
//
//open class Test1 {
//    open fun foo() {
//        if (Random.nextBoolean()) {
//            throw IllegalAccessException()
//        } else {
//            throw IllegalArgumentException()
//        }
//    }
//}
//
//class Test2 : Test1() {
//    override fun foo() {
//        throw IllegalArgumentException()
//    }
//}
//
//open class Test3 {
//    open fun foo() {
//        if (Random.nextBoolean()) {
//            throw IllegalAccessException()
//        } else {
//            throw IllegalArgumentException()
//        }
//    }
//}
//
//class Test4 : Test3() {
//    override fun foo() {
//        throw IllegalArgumentException()
//    }
//}
//
//class Test5 : Test3() {
//    override fun foo() {
//        foo()
//    }
//}
//
//abstract class Test6 {
//    abstract fun foo()
//}
//
//class Test7 : Test6() {
//    override fun foo() {
//        if (Random.nextBoolean()) {
//            throw IllegalStateException()
//        } else {
//            throw IllegalArgumentException()
//        }
//    }
//}
//
//class Test8 : Test6() {
//    override fun foo() {
//        throw IllegalStateException()
//    }
//}
//
//abstract class Test9 {
//    abstract fun foo()
//}
//
//class Test10 : Test9() {
//    override fun foo() {
//        Test11().foo()
//    }
//}
//
//class Test11 : Test9() {
//    override fun foo() {
//        Test10().foo()
//    }
//}
