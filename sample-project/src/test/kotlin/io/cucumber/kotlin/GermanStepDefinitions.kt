package io.cucumber.kotlin

import io.cucumber.java8.De
import org.junit.jupiter.api.Assertions

class GermanStepDefinitions : De {
    init {
        val alreadyHadThisManyCukes = 1
        Angenommen("ich habe {long} Gurken in meinem Bauch") { n: Long ->
            Assertions.assertEquals(1, alreadyHadThisManyCukes)
            Assertions.assertEquals(42L, n)
        }

        val localState = "hello"
        Dann("ich habe wirklich {int} Gurken in meinem Bauch") { i: Int ->
            Assertions.assertEquals(42, i)
            Assertions.assertEquals("hello", localState)
        }
        
        Dann("ich habe wirklich {stringList} Gurken in meinem Bauch") { strings: List<String> ->
            Assertions.assertEquals(listOf("foo","bar"), strings)
        }

    }
}
