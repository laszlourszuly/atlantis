package com.echsylon.atlantis.request

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be equal to`
import org.junit.Test

class PatternTest {
    @Test
    fun `When comparing two patterns with same verb expressions, true is returned`() {
        val first = Pattern(verb = "(GET|POST)")
        val second = Pattern(verb = "(GET|POST)")
        first `should be equal to` second
    }

    @Test
    fun `When comparing two patterns with different verb expressions, false is returned`() {
        val first = Pattern(verb = "(GET|POST)")
        val second = Pattern(verb = "POST")
        first `should not be equal to` second
    }

    @Test
    fun `When comparing two patterns with same path expressions, true is returned`() {
        val first = Pattern(path = ".*")
        val second = Pattern(path = ".*")
        first `should be equal to` second
    }

    @Test
    fun `When comparing two patterns with different path expressions, false is returned`() {
        val first = Pattern(path = ".*")
        val second = Pattern(path = ".+")
        first `should not be equal to` second
    }

    @Test
    fun `When comparing two patterns with same headers, true is returned`() {
        val first = Pattern().apply { headers.add("A: B") }
        val second = Pattern().apply { headers.add("A: B") }
        first `should be equal to` second
    }

    @Test
    fun `When comparing two patterns with different headers, false is returned`() {
        val first = Pattern().apply { headers.add("A: B") }
        val second = Pattern().apply { headers.add("C: D") }
        first `should not be equal to` second
    }
}