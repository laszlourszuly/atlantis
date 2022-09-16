package com.echsylon.atlantis

import com.echsylon.atlantis.Order.BATCH
import com.echsylon.atlantis.Order.SEQUENTIAL
import com.echsylon.atlantis.request.Pattern
import com.echsylon.atlantis.request.Request
import com.echsylon.atlantis.response.Message
import com.echsylon.atlantis.response.Response
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be`
import org.junit.Test

class ConfigurationTest {
    @Test
    fun `When testing for a request with matching verb, success is presented`() {
        val pattern = Pattern(verb = "(GET|POST)")
        val request = Request("GET", "/success", "HTTP/1.1")
        val response = Response()
        val configuration = Configuration()
        configuration.addResponse(pattern, response)
        configuration.findResponse(request) `should be equal to` response
    }

    @Test
    fun `When testing for a request with non-matching verb, failure is presented`() {
        val pattern = Pattern(verb = "(GET|POST)")
        val request = Request("PATCH", "/failure", "HTTP/1.1")
        val response = Response()
        val configuration = Configuration()
        configuration.addResponse(pattern, response)
        configuration.findResponse(request) `should be` null
    }

    @Test
    fun `When testing for a request with matching path, success is presented`() {
        val pattern = Pattern(path = "/success")
        val request = Request("GET", "/success", "HTTP/1.1")
        val response = Response()
        val configuration = Configuration()
        configuration.addResponse(pattern, response)
        configuration.findResponse(request) `should be equal to` response
    }

    @Test
    fun `When testing for a request with non-matching path, failure is presented`() {
        val pattern = Pattern(path = "/success")
        val request = Request("GET", "/failure", "HTTP/1.1")
        val response = Response()
        val configuration = Configuration()
        configuration.addResponse(pattern, response)
        configuration.findResponse(request) `should be` null
    }

    @Test
    fun `When testing for a request with matching headers, success is presented`() {
        val pattern = Pattern()
            .apply { headers.add("A: B") }
        val request = Request()
            .apply { headers.add("A: B") }
            .apply { headers.add("C: D") }
        val response = Response()
        val configuration = Configuration()
        configuration.addResponse(pattern, response)
        configuration.findResponse(request) `should be equal to` response
    }

    @Test
    fun `When testing for a request with non-matching headers, failure is presented`() {
        val pattern = Pattern()
            .apply { headers.add("A: B") }
        val request = Request()
            .apply { headers.add("C: D") }
            .apply { headers.add("E: F") }
        val response = Response()
        val configuration = Configuration()
        configuration.addResponse(pattern, response)
        configuration.findResponse(request) `should be` null
    }

    @Test
    fun `When requesting a response for a matching pattern, the responses are cycled by default`() {
        val pattern = Pattern("GET", "/path", "HTTP/1.1")
        val request = Request("GET", "/path", "HTTP/1.1")
        val firstResponse = Response(200)
        val secondResponse = Response(202)
        val configuration = Configuration()
        configuration.addResponse(pattern, firstResponse)
        configuration.addResponse(pattern, secondResponse)

        configuration.findResponse(request) `should be` firstResponse
        configuration.findResponse(request) `should be` secondResponse
        configuration.findResponse(request) `should be` firstResponse
    }

    @Test
    fun `When requesting a response for a matching pattern with a BATCH order, null pointer is returned`() {
        val pattern = Pattern("GET", "/path", "HTTP/1.1", BATCH)
        val request = Request("GET", "/path", "HTTP/1.1")
        val firstResponse = Response(200)
        val secondResponse = Response(202)
        val configuration = Configuration()
        configuration.addResponse(pattern, firstResponse)
        configuration.addResponse(pattern, secondResponse)

        configuration.findResponse(request) `should be` null
    }

    @Test
    fun `When requesting messages for a response in SEQUENTIAL order, the messages are returned one-by-one in a cyclic fashion`() {
        val messageA = Message(text = "A")
        val messageB = Message(text = "B")
        val messages = arrayListOf(messageA, messageB)
        val response = Response(messageOrder = SEQUENTIAL, messages = messages)
        val configuration = Configuration()
        configuration.findMessages(response).first() `should be` messageA
        configuration.findMessages(response).first() `should be` messageB
        configuration.findMessages(response).first() `should be` messageA
    }

    @Test
    fun `When requesting messages for a response in BATCH order, the messages are returned all at once`() {
        val messageA = Message(text = "A")
        val messageB = Message(text = "B")
        val messages = arrayListOf(messageA, messageB)
        val response = Response(messageOrder = BATCH, messages = messages)
        val configuration = Configuration()
        configuration.findMessages(response) `should be equal to` messages
    }

    @Test
    fun `When clearing patterns, null is returned for a previously mapped request`() {
        val pattern = Pattern("GET", "/path", "HTTP/1.1")
        val request = Request("GET", "/path", "HTTP/1.1")
        val response = Response()
        val configuration = Configuration()

        configuration.addResponse(pattern, response)
        configuration.findResponse(request) `should not be` null

        configuration.clear()
        configuration.findResponse(request) `should be` null
    }
}
