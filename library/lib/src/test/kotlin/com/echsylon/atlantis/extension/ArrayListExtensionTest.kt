package com.echsylon.atlantis.extension

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be equal to`
import org.junit.Test

class ArrayListExtensionTest {

    @Test
    fun `When checking for solid request body hint with no headers, false is returned`() {
        val header = arrayListOf<String>()
        header.expectSolidContent `should be equal to` false
    }

    @Test
    fun `When checking for chunked request body hint with no headers, false is returned`() {
        val header = arrayListOf<String>()
        header.expectChunkedContent `should be equal to` false
    }

    @Test
    fun `When checking for solid request body hint with invalid Content-Length header, false is returned`() {
        val header = arrayListOf("Content-Length: -1")
        header.expectSolidContent `should be equal to` false
    }

    @Test
    fun `When checking for chunked request body hint with invalid Transfer-Encoding header, false is returned`() {
        val header = arrayListOf("Transfer-Encoding: invalid")
        header.expectChunkedContent `should be equal to` false
    }

    @Test
    fun `When checking for solid request body hint with valid Content-Length header, true is returned`() {
        val header = arrayListOf("Content-Length: 0")
        header.expectSolidContent `should be equal to` true
    }

    @Test
    fun `When checking for chunked request body hint with valid Transfer-Encoding header, true is returned`() {
        val header = arrayListOf("Transfer-Encoding: chunked")
        header.expectChunkedContent `should be equal to` true
    }

    @Test
    fun `When fetching upgrade header, the correct value is returned`() {
        val header = arrayListOf("Upgrade: sOmE vAlUe ")
        header.upgrade `should be equal to` "sOmE vAlUe"
    }

    @Test
    fun `When fetching connection header, the correct value is returned`() {
        val header = arrayListOf("Connection: sOmE oThEr VaLuE\t")
        header.connection `should be equal to` "sOmE oThEr VaLuE"
    }

    @Test
    fun `When fetching existing Sec-WebSocket-Key header, the correct value is returned`() {
        val header = arrayListOf("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==")
        header.webSocketKey `should be equal to` "dGhlIHNhbXBsZSBub25jZQ=="
    }

    @Test
    fun `When fetching non-existing Sec-WebSocket-Key header, null is returned`() {
        val header = arrayListOf("Some-Header: value")
        header.webSocketKey `should be` null
    }

    @Test
    fun `When fetching existing Sec-WebSocket-Accept header, the correct value is returned`() {
        val header = arrayListOf("Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=")
        header.webSocketAccept `should be equal to` "s3pPLMBiTxaQ9kYGzzhZRbK+xOo="
    }

    @Test
    fun `When fetching non-existing Sec-WebSocket-Accept header, null is returned`() {
        val header = arrayListOf("Some-Header: value")
        header.webSocketAccept `should be` null
    }

    @Test
    fun `When setting new value for Sec-WebSocket-Accept header, it is correctly updated`() {
        val header = arrayListOf<String>()
        header.webSocketAccept = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo="
        header.webSocketAccept `should be equal to` "s3pPLMBiTxaQ9kYGzzhZRbK+xOo="
        header.indexOf("Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=") `should not be equal to` -1
    }
}
