package com.echsylon.atlantis

import com.echsylon.atlantis.request.Order.SEQUENTIAL
import com.echsylon.atlantis.request.Pattern
import com.echsylon.atlantis.request.Request
import com.echsylon.atlantis.response.Response
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should not be`
import org.junit.Test

class ConfigurationTest {
    private val successPattern = mockk<Pattern> {
        every { match(any<Request>()) } returns true
        every { responseOrder } returns SEQUENTIAL
    }

    private val failurePattern = mockk<Pattern> {
        every { match(any<Request>()) } returns false
        every { responseOrder } returns SEQUENTIAL
    }

    @Test
    fun `When requesting a response for a matching request pattern, the correct response is returned`() {
        val response = Response(200)
        val configuration = Configuration()
        configuration.addResponse(successPattern, response)

        val request = Request("GET", "/path", "HTTP/1.1")
        configuration.findResponse(request) `should be` response
    }

    @Test
    fun `When requesting a response for a non-matching request pattern, null is returned`() {
        val response = Response(200)
        val configuration = Configuration()
        configuration.addResponse(failurePattern, response)

        val request = Request("GET", "/path", "HTTP/1.1")
        configuration.findResponse(request) `should be` null
    }

    @Test
    fun `When requesting a response for a matching pattern, the responses are cycled by default`() {
        val firstResponse = Response(200)
        val secondResponse = Response(202)
        val configuration = Configuration()
        configuration.addResponse(successPattern, firstResponse)
        configuration.addResponse(successPattern, secondResponse)

        val request = Request("GET", "/path", "HTTP/1.1")
        configuration.findResponse(request) `should be` firstResponse
        configuration.findResponse(request) `should be` secondResponse
        configuration.findResponse(request) `should be` firstResponse
    }

    @Test
    fun `When clearing patterns, null is returned for a previously mapped request`() {
        val response = Response(200)
        val configuration = Configuration()
        val request = Request("GET", "/path", "HTTP/1.1")

        configuration.addResponse(successPattern, response)
        configuration.findResponse(request) `should not be` null

        configuration.clear()
        configuration.findResponse(request) `should be` null
    }
}