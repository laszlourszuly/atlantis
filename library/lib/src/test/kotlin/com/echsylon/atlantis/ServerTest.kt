package com.echsylon.atlantis

import com.echsylon.atlantis.header.Headers
import com.echsylon.atlantis.response.Behavior
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.lang.RuntimeException
import org.amshove.kluent.AnyException
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should not throw`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.After
import org.junit.Test
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import java.security.KeyStore
import javax.net.ssl.SSLSession
import org.junit.Before

/*
 * The test certificates used to validate the SSL features of the mock server
 * were generated with OpenSSL, using attributes similar to below example:
 *
 * Creating a private key and a certificate:
 * openssl req -newkey rsa:2048 -keyout secret.key -x509 -days 365000 -out cert.crt
 *
 * Creating an unprotected private key and a certificate:
 * openssl req -newkey rsa:2048 -nodes -keyout secret.key -x509 -days 365000 -out cert.crt
 *
 * Creating the PKCS12 trust store:
 * openssl pkcs12 -inkey secret.key -in cert.crt -export -out trust.p12 -name atlantis
 */
class ServerTest {
    // This is a temporary work-around until the mockk framework releases
    // a fix for mocking the real interfaces.
    abstract class HostnameVerifier : javax.net.ssl.HostnameVerifier
    abstract class X509TrustManager : javax.net.ssl.X509TrustManager

    private val mockVerifier = mockk<HostnameVerifier> {
        every { verify(any(), any()) } returns true
    }

    private val mockManager = mockk<X509TrustManager> {
        every { checkClientTrusted(any(), any()) } just runs
        every { checkServerTrusted(any(), any()) } just runs
        every { acceptedIssuers } returns null
    }

    private val allAcceptingSocketFactory = SSLContext.getInstance("SSL")
        .apply { init(null, arrayOf(mockManager), SecureRandom.getInstanceStrong()) }
        .socketFactory

    private lateinit var server: Server

    @Before
    fun beforeEachTests() {
        HttpsURLConnection.setDefaultHostnameVerifier(mockVerifier)
        HttpsURLConnection.setDefaultSSLSocketFactory(allAcceptingSocketFactory)
    }

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

    @Test
    fun `When supplying an unprotected trust store, its certificate is passed to the client hostname verifier`() {
        val session = slot<SSLSession>()
        val bytes = {}::class.java.getResource("/0_trust.p12").readBytes()
        val publicKey = KeyStore.getInstance("PKCS12")
            .apply { load(ByteArrayInputStream(bytes), charArrayOf()) }
            .let { it.getCertificate("atlantis") as X509Certificate }
            .publicKey

        server = Server()
        server.start(8080, bytes)
        URL("https://localhost:8080/any").openConnection()
            .let { it as HttpsURLConnection }
            .apply { responseCode }

        verify { mockVerifier.verify(any(), capture(session)) }
        invoking { session.captured.peerCertificates.last().verify(publicKey) } `should not throw` AnyException
    }

    @Test
    fun `When supplying a correct trust store, its certificate is passed to the client hostname verifier`() {
        val session = slot<SSLSession>()
        val bytes = {}::class.java.getResource("/1_trust.p12").readBytes()
        val publicKey = KeyStore.getInstance("PKCS12")
            .apply { load(ByteArrayInputStream(bytes), "password".toCharArray()) }
            .let { it.getCertificate("atlantis") as X509Certificate }
            .publicKey

        server = Server()
        server.start(8080, bytes, "password")
        URL("https://localhost:8080/any").openConnection()
            .let { it as HttpsURLConnection }
            .apply { responseCode }

        verify { mockVerifier.verify(any(), capture(session)) }
        invoking { session.captured.peerCertificates.last().verify(publicKey) } `should not throw` AnyException
    }

    @Test
    fun `When supplying a trust store with the wrong password, an exception is thrown`() {
        val bytes = {}::class.java.getResource("/1_trust.p12").readBytes()
        server = Server()
        invoking { server.start(8080, bytes, "wrong_password") } `should throw` RuntimeException::class
    }
}