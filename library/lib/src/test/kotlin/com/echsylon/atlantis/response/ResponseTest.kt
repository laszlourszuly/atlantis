package com.echsylon.atlantis.response

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be in`
import org.amshove.kluent.`should be`
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
            .apply { behavior.chunk = 1UL..1UL }
            .apply { behavior.delay = 0UL..0UL }

        response.body?.size `should be equal to` 1
        response.body?.size `should be equal to` 1
        response.body?.size `should be equal to` 1
    }

    @Test
    fun `When requesting chunked body with chunks, the correct sized chunks are delivered`() {
        val response = Response()
            .apply { headers.add("Transfer-Encoding: chunked") }
            .apply { content = byteArrayOf(0, 1, 2) }
            .apply { behavior.chunk = 1UL..1UL }
            .apply { behavior.delay = 0UL..0UL }

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
            .apply { behavior.chunk = 2UL..2UL }
            .apply { behavior.delay = 0UL..0UL }

        response.body?.size `should be equal to` 1
    }

    @Test
    fun `When requesting chunked body with chunks larger than content size, result size does not exceed content size`() {
        val response = Response()
            .apply { headers.add("Transfer-Encoding: chunked") }
            .apply { content = byteArrayOf(4, 5) }
            .apply { behavior.chunk = 4UL..4UL }
            .apply { behavior.delay = 0UL..0UL }

        // expected size is 5 because "{chunk_size}}\r\n{chunk}"
        response.body?.size `should be equal to` 5
    }

    @Test
    fun `When requesting solid body with delay, the result is delayed accordingly`() {
        val response = Response()
            .apply { headers.add("Content-Length: 3") }
            .apply { content = "Hej".encodeToByteArray() }
            .apply { behavior.chunk = 1UL..1UL }
            .apply { behavior.delay = 50UL..50UL }

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
            .apply { behavior.chunk = 1UL..1UL }
            .apply { behavior.delay = 50UL..50UL }

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
}