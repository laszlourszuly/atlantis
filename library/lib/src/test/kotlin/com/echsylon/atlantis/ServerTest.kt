package com.echsylon.atlantis

import com.echsylon.atlantis.header.Headers
import com.echsylon.atlantis.response.Behavior
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.After
import org.junit.Test
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL

class ServerTest {
    private lateinit var server: Server


    @After
    fun afterEachTest() {
        server.stop()
    }


    @Test
    fun `When mock server is started, the 'isRunning' flag returns true`() {
        server = Server()
        server.start(0)
        server.isRunning `should be` true
    }

    @Test
    fun `When mock server is started then stopped, the 'isRunning' flag returns false`() {
        server = Server()
        server.start(0)
        server.stop()
        server.isRunning `should be` false
    }

    @Test
    fun `When mock server is started and receives a known request, a mocked response code is returned`() {
        server = Server()
        server.start(8080)
        server.configuration = mockk {
            every { findResponse(any()) } returns mockk {
                every { code } returns 200
                every { phrase } returns "OK"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns Headers().apply { add("Content-Length: 0") }
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
            }
        }

        val url = URL("http://localhost:8080/success")
        val conn = url.openConnection() as HttpURLConnection
        val code = conn.responseCode
        code `should be equal to` 200
    }

    @Test
    fun `When mock server is not started, a request causes an exception to be thrown`() {
        server = Server()
        server.start(8080)
        server.stop() // For clarity
        server.configuration = mockk {
            every { findResponse(any()) } returns mockk {
                every { code } returns 404
                every { phrase } returns "Not found"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns Headers()
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
            }
        }

        val url = URL("http://localhost:8080/exception")
        val conn = url.openConnection() as HttpURLConnection
        invoking { conn.responseCode } `should throw` ConnectException::class
    }

    @Test
    fun `When response has content but no Content-Length header, the mock server adds the header with correct value`() {
        server = Server()
        server.start(8080)
        server.configuration = mockk {
            every { findResponse(any()) } returns mockk {
                every { code } returns 200
                every { phrase } returns "OK"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns Headers().apply { add("Content-Type: text/plain") }
                every { behavior } returns Behavior().apply { calculateLength = true }
                every { content } returns "Hej".encodeToByteArray()
                every { body } answers { content } andThen null
            }
        }

        val url = URL("http://localhost:8080/missing")
        val conn = url.openConnection() as HttpURLConnection
        val length = conn.getHeaderField("Content-Length")
        length `should be equal to` "3"
    }

    @Test
    fun `When response has chunked content but no Content-Length header, the mock server does not add the header`() {
        server = Server()
        server.start(8080)
        server.configuration = mockk {
            every { findResponse(any()) } returns mockk {
                every { code } returns 200
                every { phrase } returns "OK"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns Headers().apply { add("Transfer-Encoding: chunked") }
                every { behavior } returns Behavior().apply { calculateLength = true }
                every { content } returns "0x03\nHej\nhej".encodeToByteArray()
                every { body } answers { content } andThen null
            }
        }

        val url = URL("http://localhost:8080/chunked")
        val conn = url.openConnection() as HttpURLConnection
        val length = conn.getHeaderField("Content-Length")
        length `should be equal to` null
    }

    @Test
    fun `When response has Content-Length configuration, it is not overwritten by calculated length`() {
        server = Server()
        server.start(8080)
        server.configuration = mockk {
            every { findResponse(any()) } returns mockk {
                every { code } returns 200
                every { phrase } returns "OK"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns Headers().apply { add("Content-Length: 1") }
                every { behavior } returns Behavior().apply { calculateLength = true }
                every { content } returns "clearly-longer".encodeToByteArray()
                every { body } answers { content } andThen null
            }
        }

        val url = URL("http://localhost:8080/keep")
        val conn = url.openConnection() as HttpURLConnection
        val length = conn.getHeaderField("Content-Length")
        length `should be equal to` "1"
    }
}