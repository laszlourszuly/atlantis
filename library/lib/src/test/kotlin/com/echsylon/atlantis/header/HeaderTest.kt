package com.echsylon.atlantis.header

import org.amshove.kluent.`should be equal to`
import org.junit.Test

class HeaderTest {

    @Test
    fun `When checking for solid request body hint with no headers, false is returned`() {
        val header = Headers()
        header.expectSolidContent `should be equal to` false
    }

    @Test
    fun `When checking for chunked request body hint with no headers, false is returned`() {
        val header = Headers()
        header.expectChunkedContent `should be equal to` false
    }

    @Test
    fun `When checking for solid request body hint with invalid Content-Length header, false is returned`() {
        val header = Headers().apply { add("Content-Length: -1") }
        header.expectSolidContent `should be equal to` false
    }

    @Test
    fun `When checking for chunked request body hint with invalid Transfer-Encoding header, false is returned`() {
        val header = Headers().apply { add("Transfer-Encoding: invalid") }
        header.expectChunkedContent `should be equal to` false
    }

    @Test
    fun `When checking for solid request body hint with valid Content-Length header, true is returned`() {
        val header = Headers().apply { add("Content-Length: 0") }
        header.expectSolidContent `should be equal to` true
    }

    @Test
    fun `When checking for chunked request body hint with valid Transfer-Encoding header, true is returned`() {
        val header = Headers().apply { add("Transfer-Encoding: chunked") }
        header.expectChunkedContent `should be equal to` true
    }

}