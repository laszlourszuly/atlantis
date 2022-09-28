package com.echsylon.atlantis.response

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be in`
import org.junit.Test

class ResponseTest {

    @Test
    fun `When requesting body from empty content, null is returned`() {
        val response = Response().apply { headers.add("Content-Length: 0") }
        response.body `should be` null
    }

    @Test
    fun `When requesting chunked body from empty content, null is returned`() {
        val response = Response().apply { headers.add("Transfer-Encoding: chunked") }
        response.body `should be` null
    }

    @Test
    fun `When requesting solid body with chunks, the correct sized chunks are delivered`() {
        val response = Response()
            .apply { headers.add("Content-Length: 3") }
            .apply { content = byteArrayOf(0, 1, 2) }
            .apply { chunk = 1..1 }
            .apply { delay = 0..0 }

        response.body?.size `should be equal to` 1
        response.body?.size `should be equal to` 1
        response.body?.size `should be equal to` 1
    }

    @Test
    fun `When requesting chunked body with chunks, the correct sized chunks are delivered`() {
        val response = Response()
            .apply { headers.add("Transfer-Encoding: chunked") }
            .apply { content = byteArrayOf(0, 1, 2) }
            .apply { chunk = 1..1 }
            .apply { delay = 0..0 }

        // expected size is 4 because body="{chunk_size}\r\n{chunk}"
        response.body?.size `should be equal to` 4
        response.body?.size `should be equal to` 4
        response.body?.size `should be equal to` 4
    }

    @Test
    fun `When requesting solid body with chunks larger than content size, result size does not exceed content size`() {
        val response = Response()
            .apply { headers.add("Content-Length: 1") }
            .apply { content = byteArrayOf(3) }
            .apply { chunk = 2..2 }
            .apply { delay = 0..0 }

        response.body?.size `should be equal to` 1
    }

    @Test
    fun `When requesting chunked body with chunks larger than content size, result size does not exceed content size`() {
        val response = Response()
            .apply { headers.add("Transfer-Encoding: chunked") }
            .apply { content = byteArrayOf(4, 5) }
            .apply { chunk = 4..4 }
            .apply { delay = 0..0 }

        // expected size is 5 because "{chunk_size}\r\n{chunk}"
        response.body?.size `should be equal to` 5
    }

    @Test
    fun `When requesting solid body with delay, the result is delayed accordingly`() {
        val response = Response()
            .apply { headers.add("Content-Length: 3") }
            .apply { content = "Hej".encodeToByteArray() }
            .apply { chunk = 1..1 }
            .apply { delay = 50..50 }

        val start = System.currentTimeMillis()
        response.body
        val delay = System.currentTimeMillis() - start
        delay `should be in` 20L..80L // allowing 30ms difference
    }

    @Test
    fun `When requesting chunked body with delay, the result is delayed accordingly`() {
        val response = Response()
            .apply { headers.add("Transfer-Encoding: chunked") }
            .apply { content = "Hej".encodeToByteArray() }
            .apply { chunk = 1..1 }
            .apply { delay = 50..50 }

        val start = System.currentTimeMillis()
        response.body
        val delay = System.currentTimeMillis() - start
        delay `should be in` 20L..80L // allowing 30ms difference
    }

    @Test
    fun `When reading body for a solid response, the content is presented in its entirety`() {
        val response = Response()
            .apply { headers.add("Content-Length : 3") }
            .apply { content = "Hej".encodeToByteArray() }

        response.body!!.decodeToString() `should be equal to` "Hej"
    }

    @Test
    fun `When reading body for a chunked response, the content is presented in chunks`() {
        val response = Response()
            .apply { headers.add("Transfer-Encoding : chunked") }
            .apply { content = "Hej".encodeToByteArray() }
        response.body!!.decodeToString() `should be equal to` "3\r\nHej"
    }

    @Test
    fun `When reading body for a response without headers, the content is presented in its entirety`() {
        val response = Response()
            .apply { content = "Hej".encodeToByteArray() }
        response.body!!.decodeToString() `should be equal to` "Hej"
    }

    @Test
    fun `When no Upgrade header is defined, then isWebSocket returns false`() {
        val response = Response(101)
            .apply { headers.add("Connection: Upgrade") }
        response.isWebSocket `should be equal to` false
    }

    @Test
    fun `When wrong Upgrade header is defined, then isWebSocket returns false`() {
        val response = Response(101)
            .apply { headers.add("Upgrade: wrong") }
            .apply { headers.add("Connection: Upgrade") }
        response.isWebSocket `should be equal to` false
    }

    @Test
    fun `When no Connection header is defined, then isWebSocket returns false`() {
        val response = Response(101)
            .apply { headers.add("Upgrade: websocket") }
        response.isWebSocket `should be equal to` false
    }

    @Test
    fun `When wrong Connection header is defined, then isWebSocket returns false`() {
        val response = Response(101)
            .apply { headers.add("Upgrade: websocket") }
            .apply { headers.add("Connection: wrong") }
        response.isWebSocket `should be equal to` false
    }

    @Test
    fun `When wrong status code, then isWebSocket returns false`() {
        val response = Response(200)
            .apply { headers.add("Upgrade: websocket") }
            .apply { headers.add("Connection: Upgrade") }
        response.isWebSocket `should be equal to` false
    }

    @Test
    fun `When correct status code and Upgrade header and Connection header, then isWebSocket returns true`() {
        val response = Response(101)
            .apply { headers.add("Upgrade: websocket") }
            .apply { headers.add("Connection: Upgrade") }
        response.isWebSocket `should be equal to` true
    }
}
