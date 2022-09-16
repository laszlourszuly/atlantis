package com.echsylon.atlantis

import com.echsylon.atlantis.Order.BATCH
import com.echsylon.atlantis.Order.SEQUENTIAL
import com.echsylon.atlantis.message.Type.CLOSE
import com.echsylon.atlantis.request.Request
import com.echsylon.atlantis.response.Behavior
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.amshove.kluent.AnyException
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain same`
import org.amshove.kluent.`should not throw`
import org.amshove.kluent.`should throw`
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.ConnectException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory

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

    private lateinit var mockVerifier: HostnameVerifier
    private lateinit var mockManager: X509TrustManager
    private lateinit var socketFactory: SSLSocketFactory
    private lateinit var server: Server
    private lateinit var client: OkHttpClient

    @Before
    fun beforeEachTest() {
        mockVerifier = mockk {
            every { verify(any(), any()) } returns true
        }

        mockManager = mockk {
            every { checkClientTrusted(any(), any()) } just runs
            every { checkServerTrusted(any(), any()) } just runs
            every { acceptedIssuers } returns emptyArray()
        }

        socketFactory = SSLContext.getInstance("SSL")
            .apply { init(null, arrayOf(mockManager), SecureRandom.getInstanceStrong()) }
            .socketFactory

        client = OkHttpClient.Builder()
            .readTimeout(0, MILLISECONDS)
            .writeTimeout(0, MILLISECONDS)
            .connectionPool(ConnectionPool(0, 1, MILLISECONDS))
            .hostnameVerifier(mockVerifier)
            .sslSocketFactory(socketFactory, mockManager)
            .build()

    }

    @After
    fun afterEachTest() {
        server.stop()
        client.dispatcher.cancelAll()
        client.dispatcher.executorService.shutdownNow()
        client.dispatcher.executorService.awaitTermination(1000L, MILLISECONDS)
        clearAllMocks()
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
    fun `When supplying an unprotected trust store, its certificate is passed to the client hostname verifier`() {
        val session = slot<SSLSession>()
        val bytes = {}::class.java.getResource("/0_trust.p12")?.readBytes()
        val publicKey = KeyStore.getInstance("PKCS12")
            .apply { load(ByteArrayInputStream(bytes), charArrayOf()) }
            .let { it.getCertificate("atlantis") as X509Certificate }
            .publicKey
        val request = okhttp3.Request.Builder()
            .url("https://localhost:8080/any")
            .build()

        server = Server()
        server.start(8080, bytes)
        client.newCall(request).execute().close()
        verify { mockVerifier.verify(any(), capture(session)) }
        invoking { session.captured.peerCertificates.last().verify(publicKey) } `should not throw` AnyException
    }

    @Test
    fun `When supplying a correct trust store, its certificate is passed to the client hostname verifier`() {
        val session = slot<SSLSession>()
        val bytes = {}::class.java.getResource("/1_trust.p12")?.readBytes()
        val publicKey = KeyStore.getInstance("PKCS12")
            .apply { load(ByteArrayInputStream(bytes), "password".toCharArray()) }
            .let { it.getCertificate("atlantis") as X509Certificate }
            .publicKey
        val request = okhttp3.Request.Builder()
            .url("https://localhost:8080/any")
            .build()

        server = Server()
        server.start(8080, bytes, "password")
        client.newCall(request).execute().close()
        verify { mockVerifier.verify(any(), capture(session)) }
        invoking { session.captured.peerCertificates.last().verify(publicKey) } `should not throw` AnyException
    }

    @Test
    fun `When supplying a trust store with the wrong password, an exception is thrown`() {
        val bytes = {}::class.java.getResource("/1_trust.p12")?.readBytes()
        server = Server()
        invoking { server.start(8080, bytes, "wrong_password") } `should throw` RuntimeException::class
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
                every { headers } returns arrayListOf("Content-Length: 0")
                every { behavior } returns Behavior()
                every { body } answers { byteArrayOf() } andThen null
                every { isWebSocket } returns false
                every { hasMessages } returns false
            }
        }

        val request = okhttp3.Request.Builder()
            .url("http://localhost:8080/success")
            .build()

        client
            .newCall(request)
            .execute()
            .use { it.code `should be equal to` 200 }
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
                every { headers } returns arrayListOf()
                every { behavior } returns Behavior()
                every { body } answers { byteArrayOf() } andThen null
                every { isWebSocket } returns false
                every { hasMessages } returns false
            }
        }

        val request = okhttp3.Request.Builder()
            .url("http://localhost:8080/success")
            .build()

        invoking { client.newCall(request).execute().close() } `should throw` ConnectException::class
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
                every { headers } returns arrayListOf("Content-Type: text/plain")
                every { behavior } returns Behavior()
                every { content } returns "Hej".encodeToByteArray()
                every { body } answers { content } andThen null
                every { isWebSocket } returns false
                every { hasMessages } returns false
            }
        }

        val request = okhttp3.Request.Builder()
            .url("http://localhost:8080/missing-content-length-header-definition")
            .build()

        client
            .newCall(request)
            .execute()
            .use { it.header("Content-Length") `should be equal to` "3" }
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
                every { headers } returns arrayListOf("Transfer-Encoding: chunked")
                every { behavior } returns Behavior()
                every { body } answers { "0x03\nHej\nhej".encodeToByteArray() } andThen null
                every { isWebSocket } returns false
                every { hasMessages } returns false
            }
        }

        val request = okhttp3.Request.Builder()
            .url("http://localhost:8080/no-content-length-for-chunked-response")
            .build()

        client
            .newCall(request)
            .execute()
            .use { it.header("Content-Length") `should be` null }
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
                every { headers } returns arrayListOf("Content-Length: 1")
                every { behavior } returns Behavior()
                every { body } answers { "clearly-longer".encodeToByteArray() } andThen null
                every { isWebSocket } returns false
                every { hasMessages } returns false
            }
        }

        val request = okhttp3.Request.Builder()
            .url("http://localhost:8080/static-content-length-header-definition")
            .build()

        client
            .newCall(request)
            .execute()
            .use { it.header("Content-Length") `should be equal to` "1" }
    }

    @Test
    fun `When response has no Sec-WebSocket-Accept header, it is calculated correctly`() {
        server = Server()
        server.start(8080)
        server.configuration = mockk {
            every { findResponse(any()) } returns mockk {
                every { code } returns 101
                every { phrase } returns "Switching Protocols"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf()
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns false
                every { hasMessages } returns false
            }
        }

        val request = okhttp3.Request.Builder()
            .url("http://localhost:8080/ws/messages/1")
            .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
            .build()

        client
            .newCall(request)
            .execute()
            .use { it.header("Sec-WebSocket-Accept") `should be equal to` "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=" }
    }

    @Test
    @Ignore
    // This test is questionable. It's testing the client implementation.
    fun `When request is websocket endpoint and response is accept, the client is notified about websocket open`() {
        val configuration: Configuration = mockk {
            every { findResponse(any()) } returns mockk {
                every { code } returns 101
                every { phrase } returns "Switching Protocols"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf("Upgrade: websocket", "Connection: Upgrade")
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns true
                every { hasMessages } returns false
                every { messageOrder } returns SEQUENTIAL
                every { messages } returns arrayListOf()
            }
        }

        val handshakeRequest = okhttp3.Request.Builder()
            .url("ws://localhost:8080/ws/messages/2")
            .build()

        val opened = slot<okhttp3.Response>()
        val callback: okhttp3.WebSocketListener = mockk {
            every { onClosing(any(), any(), any()) } just runs
            every { onOpen(any(), capture(opened)) } just runs
        }

        server = Server()
        server.start(8080)
        server.configuration = configuration
        client.newWebSocket(handshakeRequest, callback)
        verify(exactly = 1, timeout = 400) { callback.onOpen(any(), any()) }
        opened.captured.code `should be equal to` 101

        // The client is closing the connection ungracefully when exiting this
        // test, not giving Atlantis a fair chance to handle the departure.
        // We'll see a post-verification print in the logs which has no effect
        // on the test itself.
    }

    @Test
    @Ignore
    // This test is questionable. It's testing the client implementation.
    fun `When request is websocket endpoint and response is NOT accept, the client is notified about websocket failure`() {
        val configuration: Configuration = mockk {
            every { findResponse(any()) } returns mockk {
                every { code } returns 400
                every { phrase } returns "Bad Request"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf("Upgrade: websocket", "Connection: Upgrade")
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns true
                every { hasMessages } returns false
            }
        }

        val handshakeRequest = okhttp3.Request.Builder()
            .url("ws://localhost:8080/ws/messages/3")
            .build()

        val failed = slot<okhttp3.Response>()
        val callback: okhttp3.WebSocketListener = mockk {
            every { onFailure(any(), any(), capture(failed)) } just runs
        }

        server = Server()
        server.start(8080)
        server.configuration = configuration
        client.newWebSocket(handshakeRequest, callback)
        verify(exactly = 1, timeout = 400) { callback.onFailure(any(), any(), any()) }
        failed.captured.code `should be equal to` 400
    }

    @Test
    @Ignore
    fun `When request is websocket endpoint and response has a single message, the message is sent on connect`() {
        val configuration: Configuration = mockk {
            every { findMessages(any()) } answers { callOriginal() }
            every { findResponse(any()) } returns mockk {
                every { code } returns 101
                every { phrase } returns "Switching Protocols"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf("Upgrade: websocket", "Connection: Upgrade")
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns true
                every { hasMessages } returns true
                every { messageOrder } returns SEQUENTIAL
                every { messages } returns arrayListOf(mockk {
                    every { path } returns "/ws/messages/4"
                    every { text } returns "Hello there!"
                    every { type } returns null
                    every { code } returns null
                    every { chunk } returns 0..0
                    every { delay } returns 0..0
                })
            }
        }

        val handshakeRequest = okhttp3.Request.Builder()
            .url("ws://localhost:8080/ws/messages/4")
            .build()

        val message = slot<String>()
        val callback: okhttp3.WebSocketListener = mockk {
            every { onFailure(any(), any(), any()) } just runs
            every { onMessage(any(), capture(message)) } just runs
            every { onOpen(any(), any()) } just runs
        }

        server = Server()
        server.start(8080)
        server.configuration = configuration
        client.newWebSocket(handshakeRequest, callback)
        verify(exactly = 1, timeout = 400) { callback.onMessage(any(), any<String>()) }
        message.captured `should be equal to` "Hello there!"
    }

    @Test
    @Ignore
    fun `When request is websocket endpoint and response has a batch of messages, all messages are sent on connect`() {
        val configuration: Configuration = mockk {
            every { findMessages(any()) } answers { callOriginal() }
            every { findResponse(any()) } returns mockk {
                every { code } returns 101
                every { phrase } returns "Switching Protocols"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf("Upgrade: websocket", "Connection: Upgrade")
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns true
                every { hasMessages } returns true
                every { messageOrder } returns BATCH
                every { messages } returns arrayListOf(mockk {
                    every { path } returns "/ws/messages/5"
                    every { text } returns "Hello1"
                    every { type } returns null
                    every { code } returns null
                    every { chunk } returns 0..0
                    every { delay } returns 0..0
                }, mockk {
                    every { path } returns "/ws/messages/5"
                    every { text } returns "Hello2"
                    every { type } returns null
                    every { code } returns null
                    every { chunk } returns 0..0
                    every { delay } returns 0..0
                })
            }
        }

        val handshakeRequest = okhttp3.Request.Builder()
            .url("ws://localhost:8080/ws/messages/5")
            .build()

        val messages = mutableListOf<String>()
        val callback: okhttp3.WebSocketListener = mockk {
            every { onFailure(any(), any(), any()) } just runs
            every { onMessage(any(), capture(messages)) } just runs
            every { onOpen(any(), any()) } just runs
        }

        server = Server()
        server.start(8080)
        server.configuration = configuration
        client.newWebSocket(handshakeRequest, callback)
        verify(exactly = 2, timeout = 400) { callback.onMessage(any(), any<String>()) }
        messages `should contain same` listOf("Hello1", "Hello2")
    }

    @Test
    @Ignore
    fun `When request is not websocket endpoint and response has a single message, the message is sent on response`() {
        val handshake: Request = mockk {
            every { verb } returns "GET"
            every { path } returns "/ws/messages/6"
            every { protocol } returns "HTTP/1.1"
        }
        val request: Request = mockk {
            every { verb } returns "GET"
            every { path } returns "/api/update"
            every { protocol } returns "HTTP/1.1"
        }
        val configuration: Configuration = mockk {
            every { findMessages(any()) } answers { callOriginal() }
            every { findResponse(handshake) } returns mockk {
                every { code } returns 101
                every { phrase } returns "Switching Protocols"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf("Upgrade: websocket", "Connection: Upgrade")
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns true
                every { hasMessages } returns false
            }
            every { findResponse(request) } returns mockk {
                every { code } returns 202
                every { phrase } returns "Accepted"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf()
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns false
                every { hasMessages } returns true
                every { messageOrder } returns SEQUENTIAL
                every { messages } returns arrayListOf(mockk {
                    every { path } returns "/ws/messages/6"
                    every { text } returns "Hello3"
                    every { type } returns null
                    every { code } returns null
                    every { chunk } returns 0..0
                    every { delay } returns 0..0
                })
            }
        }

        val handshakeRequest = okhttp3.Request.Builder()
            .url("ws://localhost:8080/ws/messages/6")
            .build()

        val triggerRequest = okhttp3.Request.Builder()
            .url("http://localhost:8080/api/update")
            .build()

        val message = slot<String>()
        val callback: okhttp3.WebSocketListener = mockk {
            every { onFailure(any(), any(), any()) } just runs
            every { onMessage(any(), capture(message)) } just runs
            every { onOpen(any(), any()) } answers {
                client.newCall(triggerRequest).execute().close()
            }
        }

        server = Server()
        server.start(8080)
        server.configuration = configuration
        client.newWebSocket(handshakeRequest, callback)
        verify(exactly = 1, timeout = 400) { callback.onMessage(any(), any<String>()) }
        message.captured `should be equal to` "Hello3"
    }

    @Test
    @Ignore
    fun `When request is not websocket endpoint and response has a batch of messages, all messages are sent on response`() {
        val handshake: Request = mockk {
            every { verb } returns "GET"
            every { path } returns "/ws/messages/6"
            every { protocol } returns "HTTP/1.1"
        }
        val request: Request = mockk {
            every { verb } returns "GET"
            every { path } returns "/api/update"
            every { protocol } returns "HTTP/1.1"
        }
        val configuration: Configuration = mockk {
            every { findMessages(any()) } answers { callOriginal() }
            every { findResponse(handshake) } returns mockk {
                every { code } returns 101
                every { phrase } returns "Switching Protocols"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf("Upgrade: websocket", "Connection: Upgrade")
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns true
                every { hasMessages } returns false
            }
            every { findResponse(request) } returns mockk {
                every { code } returns 202
                every { phrase } returns "Accepted"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf()
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns false
                every { hasMessages } returns true
                every { messageOrder } returns BATCH
                every { messages } returns arrayListOf(mockk {
                    every { path } returns "/ws/messages/6"
                    every { text } returns "Hello4"
                    every { type } returns null
                    every { code } returns null
                    every { chunk } returns 0..0
                    every { delay } returns 0..0
                }, mockk {
                    every { path } returns "/ws/messages/6"
                    every { text } returns "Hello5"
                    every { type } returns null
                    every { code } returns null
                    every { chunk } returns 0..0
                    every { delay } returns 0..0
                })
            }
        }

        val handshakeRequest = okhttp3.Request.Builder()
            .url("ws://localhost:8080/ws/messages/6")
            .build()

        val triggerRequest = okhttp3.Request.Builder()
            .url("http://localhost:8080/api/update")
            .build()

        val messages = mutableListOf<String>()
        val callback: okhttp3.WebSocketListener = mockk {
            every { onMessage(any(), capture(messages)) } just runs
            every { onOpen(any(), any()) } answers {
                client.newCall(triggerRequest).execute().close()
            }
        }

        server = Server()
        server.start(8080)
        server.configuration = configuration
        client.newWebSocket(handshakeRequest, callback)
        verify(exactly = 2, timeout = 400) { callback.onMessage(any(), any<String>()) }
        messages `should contain same` listOf("Hello4", "Hello5")
    }

    @Test
    @Ignore
    fun `When request is not websocket endpoint and response has a single close message, web socket is closed gracefully`() {
        val handshake: Request = mockk {
            every { verb } returns "GET"
            every { path } returns "/ws/messages/7"
            every { protocol } returns "HTTP/1.1"
        }
        val request: Request = mockk {
            every { verb } returns "GET"
            every { path } returns "/api/update/2"
            every { protocol } returns "HTTP/1.1"
        }
        val configuration: Configuration = mockk {
            every { findMessages(any()) } answers { callOriginal() }
            every { findResponse(handshake) } returns mockk {
                every { code } returns 101
                every { phrase } returns "Switching Protocols"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf("Upgrade: websocket", "Connection: Upgrade")
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns true
                every { hasMessages } returns false
            }
            every { findResponse(request) } returns mockk {
                every { code } returns 202
                every { phrase } returns "Accepted"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf()
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns false
                every { hasMessages } returns true
                every { messageOrder } returns SEQUENTIAL
                every { messages } returns arrayListOf(mockk {
                    every { path } returns "/ws/messages/7"
                    every { text } returns "Bye"
                    every { type } returns CLOSE
                    every { code } returns 1001
                    every { chunk } returns 0..0
                    every { delay } returns 0..0
                })
            }
        }

        val handshakeRequest = okhttp3.Request.Builder()
            .url("ws://localhost:8080/ws/messages/7")
            .build()

        val triggerRequest = okhttp3.Request.Builder()
            .url("http://localhost:8080/api/update/2")
            .build()

        val reason = slot<Int>()
        val message = slot<String>()
        val callback: okhttp3.WebSocketListener = mockk {
            every { onClosing(any(), capture(reason), capture(message)) } just runs
            every { onOpen(any(), any()) } answers {
                client.newCall(triggerRequest).execute().close()
            }
        }

        server = Server()
        server.start(8080)
        server.configuration = configuration
        client.newWebSocket(handshakeRequest, callback)
        verify(exactly = 1, timeout = 400) { callback.onClosing(any(), any(), any()) }
        reason.captured `should be equal to` 1001
        message.captured `should be equal to` "Bye"
    }

    @Test
    @Ignore
    fun `When request is not websocket endpoint and response has a batch of messages, no further messages are sent after close message`() {
        val handshake: Request = mockk {
            every { verb } returns "GET"
            every { path } returns "/ws/messages/8"
            every { protocol } returns "HTTP/1.1"
        }
        val request: Request = mockk {
            every { verb } returns "GET"
            every { path } returns "/api/update/3"
            every { protocol } returns "HTTP/1.1"
        }
        val configuration: Configuration = mockk {
            every { findMessages(any()) } answers { callOriginal() }
            every { findResponse(handshake) } returns mockk {
                every { code } returns 101
                every { phrase } returns "Switching Protocols"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf("Upgrade: websocket", "Connection: Upgrade")
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns true
                every { hasMessages } returns false
            }
            every { findResponse(request) } returns mockk {
                every { code } returns 202
                every { phrase } returns "Accepted"
                every { protocol } returns "HTTP/1.1"
                every { headers } returns arrayListOf()
                every { behavior } returns Behavior()
                every { content } returns byteArrayOf()
                every { body } answers { content } andThen null
                every { isWebSocket } returns false
                every { hasMessages } returns true
                every { messageOrder } returns BATCH
                every { messages } returns arrayListOf(mockk {
                    every { path } returns "/ws/messages/8"
                    every { text } returns "Hello6"
                    every { type } returns null
                    every { code } returns null
                    every { chunk } returns 0..0
                    every { delay } returns 0..0
                }, mockk {
                    every { path } returns "/ws/messages/8"
                    every { text } returns "Bye"
                    every { type } returns CLOSE
                    every { code } returns 1001
                    every { chunk } returns 0..0
                    every { delay } returns 0..0
                }, mockk {
                    every { path } returns "/ws/messages/8"
                    every { text } returns "Hello7"
                    every { type } returns null
                    every { code } returns null
                    every { chunk } returns 0..0
                    every { delay } returns 0..0
                })
            }
        }

        val handshakeRequest = okhttp3.Request.Builder()
            .url("ws://localhost:8080/ws/messages/8")
            .build()

        val triggerRequest = okhttp3.Request.Builder()
            .url("http://localhost:8080/api/update/3")
            .build()

        val message = slot<String>()
        val callback: okhttp3.WebSocketListener = mockk {
            every { onMessage(any(), capture(message)) } just runs
            every { onClosing(any(), any(), any()) } just runs
            every { onOpen(any(), any()) } answers {
                client.newCall(triggerRequest).execute().close()
            }
        }

        server = Server()
        server.start(8080)
        server.configuration = configuration
        client.newWebSocket(handshakeRequest, callback)
        verify(exactly = 1, timeout = 400) { callback.onMessage(any(), any<String>()) }
        message.captured `should be equal to` "Hello6"
    }
}
