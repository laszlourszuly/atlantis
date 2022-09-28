package com.echsylon.atlantis

import com.echsylon.atlantis.extension.closeSilently
import com.echsylon.atlantis.extension.contentLength
import com.echsylon.atlantis.extension.expectChunkedContent
import com.echsylon.atlantis.extension.expectSolidContent
import com.echsylon.atlantis.extension.webSocketAccept
import com.echsylon.atlantis.extension.webSocketKey
import com.echsylon.atlantis.message.WebSocketHelper
import com.echsylon.atlantis.request.Request
import com.echsylon.atlantis.response.Response
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.util.Base64
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.net.ServerSocketFactory
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.TrustManagerFactory
import kotlin.text.Charsets.UTF_8

/**
 * Represents the remote server and acts as the heart of Atlantis. It listens
 * for client HTTP requests and serves corresponding mock responses as defined
 * in the current configuration. The default configuration for this server will
 * serve "404 Not found" on all requests that won't match a configured pattern.
 */
internal class Server {
    companion object {
        private const val WS_ACCEPT_UUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        internal val NOT_FOUND = Response(404)
    }

    private var server: ServerSocket? = null
    private var config: Configuration = Configuration()
    private val webSocket: WebSocketHelper = WebSocketHelper { key, bytes, isText ->
        handleWebSocketMessage(key, bytes, isText)
    }
    private val executor: ExecutorService = Executors.newCachedThreadPool {
        Thread(it, "Atlantis Mock Server").apply { isDaemon = true }
    }

    /**
     * The current request pattern configuration.
     */
    var configuration: Configuration
        get() = config
        set(value) {
            config = value
        }

    /**
     * Whether the mock server is currently active or not.
     */
    val isRunning: Boolean
        get() = server != null &&
                server?.isClosed == false

    /**
     * Start listening for requests and serving mock responses for them.
     *
     * The trust store is passed as-is, directly to the JVM trust store
     * implementation. It is expected to be provided as a single keystore
     * containing one X509 certificate and one PKCS12 secret key.
     *
     * @param port The port to serve mock data on.
     * @param trust The trust store byte array.
     * @param password The trust store password, or null.
     * @throws RuntimeException if anything goes wrong.
     * @return True if the server was successfully started, else false.
     */
    fun start(port: Int, trust: ByteArray? = null, password: String? = null) {
        if (isRunning) return
        server = runCatching { createServerSocket(port, trust, password) }
            .onFailure { stop() }
            .getOrThrow()

        executor.execute {
            do {
                server
                    ?.runCatching { serveConnection(accept()) }
                    ?.onFailure { stop() }
            } while (isRunning)
        }
    }

    /**
     * Stops serving mock responses.
     *
     * @return True if the server was successfully stopped, else false.
     */
    fun stop() {
        webSocket.stopAll()
        executor.shutdownNow()
        server?.closeSilently()
        server = null
    }

    /**
     * Tries to create a suitable server socket based on the given parameters.
     *
     * @param port The port to serve mock data on.
     * @param trust The trust store byte array.
     * @param password The trust store password, or null.
     * @return A default [ServerSocket] if no trust input stream is given. An
     * [SSLServerSocket] otherwise.
     */
    private fun createServerSocket(port: Int, trust: ByteArray?, password: String?): ServerSocket {
        return when (trust) {
            null -> createDefaultServerSocket(port)
            else -> createSecureServerSocket(port, trust, password)
        }
    }

    /**
     * Creates a new server socket and tries to bind it to the given port.
     *
     * @param port The port to serve mock data on.
     * @throws RuntimeException if anything goes wrong.
     * @return The bound server socket.
     */
    private fun createDefaultServerSocket(port: Int): ServerSocket {
        try {
            // Passing in a null pointer InetAddress will force the ServerSocket
            // to assume the "wildcard" address (ultimately "localhost") as host,
            // with the additional benefit of not attempting to resolve it on the
            // network.
            val inetAddress: InetAddress? = null
            val inetSocketAddress = InetSocketAddress(inetAddress, port)
            return ServerSocketFactory.getDefault()
                .createServerSocket()
                .apply { reuseAddress = inetSocketAddress.port != 0 }
                .apply { bind(inetSocketAddress) }
        } catch (cause: SocketException) {
            throw RuntimeException("Could not determine address reuse strategy", cause)
        } catch (cause: IOException) {
            throw RuntimeException("Unexpected connection error", cause)
        } catch (cause: SecurityException) {
            throw RuntimeException("Not allowed to connect to port $port", cause)
        } catch (cause: IllegalArgumentException) {
            throw RuntimeException("Could not connect to port $port", cause)
        }
    }

    /**
     * Creates a new server socket and tries to bind it to the given address
     * and port.
     *
     * @param port The port to serve mock data on.
     * @param trust The trust store byte array.
     * @param password The trust store password, or null.
     * @throws RuntimeException if anything goes wrong.
     * @return The bound server socket.
     */
    private fun createSecureServerSocket(port: Int, trust: ByteArray, password: String?): SSLServerSocket {
        try {
            // Passing in a null pointer InetAddress will force the ServerSocket
            // to assume the "wildcard" address (ultimately "localhost") as host,
            // with the additional benefit of not attempting to resolve it on the
            // network.
            val inetAddress: InetAddress? = null
            val inetSocketAddress = InetSocketAddress(inetAddress, port)
            val secureContext = createSecureContext(trust, password)
            return secureContext.serverSocketFactory
                .createServerSocket()
                .let { it as SSLServerSocket }
                .apply { reuseAddress = inetSocketAddress.port != 0 }
                .apply { bind(inetSocketAddress) }
        } catch (cause: SocketException) {
            throw RuntimeException("Could not determine address reuse strategy", cause)
        } catch (cause: IOException) {
            throw RuntimeException("Unexpected connection error", cause)
        } catch (cause: SecurityException) {
            throw RuntimeException("Not allowed to connect to port $port", cause)
        } catch (cause: IllegalArgumentException) {
            throw RuntimeException("Could not connect to port $port", cause)
        }
    }

    /**
     * Creates an instance of SSLContext with custom key and trust managers.
     *
     * @param trust The trust store byte array.
     * @param password The trust store password, or null.
     * @throws RuntimeException if anything goes wrong.
     * @return The prepared SSL context.
     */
    private fun createSecureContext(trust: ByteArray, password: String?): SSLContext {
        try {
            val trustFactory = createTrustFactory(trust, password)
            val keyFactory = createKeyFactory(trust, password)
            val context = SSLContext.getInstance("TLS")
            context.init(keyFactory.keyManagers, trustFactory.trustManagers, SecureRandom.getInstanceStrong())
            return context
        } catch (cause: NoSuchAlgorithmException) {
            throw RuntimeException("Could not create secure context", cause)
        } catch (cause: KeyManagementException) {
            throw RuntimeException("Could not initialize secure context", cause)
        }
    }

    /**
     * Creates a custom trust factory.
     *
     * @param trust The trust store byte array.
     * @param password The corresponding password.
     * @throws RuntimeException if anything goes wrong.
     * @return The trust factory that will provide the given certificate data.
     */
    private fun createTrustFactory(trust: ByteArray, password: String?): TrustManagerFactory {
        try {
            val trustFactory = TrustManagerFactory.getInstance("PKIX")
            val keyStore = KeyStore.getInstance("PKCS12")
            val input = ByteArrayInputStream(trust)
            val pwd = password?.toCharArray() ?: charArrayOf()
            keyStore.load(input, pwd)
            trustFactory.init(keyStore)
            return trustFactory
        } catch (cause: NoSuchAlgorithmException) {
            throw RuntimeException("Could not initialize trust factory", cause)
        } catch (cause: KeyStoreException) {
            throw RuntimeException("Could not initialize trust factory", cause)
        } catch (cause: CertificateException) {
            throw RuntimeException("Could not load certificate", cause)
        } catch (cause: IOException) {
            throw RuntimeException("Could not load certificate", cause)
        }
    }

    /**
     * Creates a custom key factory.
     *
     * @param key The secret key store byte array.
     * @param password The trust store password, or null.
     * @throws RuntimeException if anything goes wrong.
     * @return The key factory that will provide the given secret key data.
     */
    private fun createKeyFactory(key: ByteArray, password: String?): KeyManagerFactory {
        try {
            val keyFactory = KeyManagerFactory.getInstance("PKIX")
            val keyStore = KeyStore.getInstance("PKCS12")
            val input = ByteArrayInputStream(key)
            val pwd = password?.toCharArray() ?: charArrayOf()
            keyStore.load(input, pwd)
            keyFactory.init(keyStore, pwd)
            return keyFactory
        } catch (cause: NoSuchAlgorithmException) {
            throw RuntimeException("Could not initialize trust factory", cause)
        } catch (cause: KeyStoreException) {
            throw RuntimeException("Could not initialize trust factory", cause)
        } catch (cause: UnrecoverableKeyException) {
            throw RuntimeException("Could not initialize trust factory", cause)
        } catch (cause: CertificateException) {
            throw RuntimeException("Could not load certificate", cause)
        } catch (cause: IOException) {
            throw RuntimeException("Could not load certificate", cause)
        }
    }

    /**
     * Waits for a client to request a connection to the server socket and
     * serves a mocked response for any read requests.
     *
     * @param socket The socket associated with the client request. This socket
     * will also be used to serve any websocket messages on, in which case it
     * will be blocked until the websocket is closed.
     *
     * @throws RuntimeException if anything goes wrong.
     */
    private fun serveConnection(socket: Socket) {
        executor.execute {
            runCatching {
                val source = socket.source().buffer()
                val request = readRequestSignature(source)
                request.headers.addAll(readRequestHeaders(source))
                request.content = readRequestContent(request, source)
                println("Atlantis Request:\n\t$request")

                val response = config.findResponse(request) ?: NOT_FOUND
                if (response.behavior.calculateLength)
                    ensureContentLength(response)

                val webSocketKey = request.headers.webSocketKey
                if (response.behavior.calculateWsAccept)
                    ensureSecWebSocketAccept(webSocketKey, response)

                println("Atlantis Response:\n\t$response")
                val sink = socket.sink().buffer()
                writeResponseSignature(response, sink)
                writeResponseHeaders(response, sink)
                writeResponseContent(response, sink)

                if (response.hasMessages) {
                    config.findMessages(response)
                        .groupBy { it.path }
                        .forEach { (key, messages) ->
                            webSocket.send(key, *messages.toTypedArray())
                        }
                }

                if (response.isWebSocket) {
                    socket.soTimeout = 0
                    webSocket.start(request.path, source, sink)
                }
            }.onFailure {
                it.printStackTrace()
            }.also {
                socket.closeSilently()
            }
        }
    }

    /**
     * Reads the signature line from the request stream.
     *
     * Example:
     *   "GET /path/to/resource HTTP/1.1"
     *
     * @param input The data source stream to read from.
     * @return The request object with the bare minimum signature.
     *
     * @throws IOException if the source can't be read.
     * @throws IllegalArgumentException if the source is empty.
     */
    private fun readRequestSignature(input: BufferedSource): Request {
        val signature = input.readUtf8LineStrict()
        if (signature.isEmpty()) throw IllegalArgumentException("Empty request")
        val (method, path, protocol) = signature.split(" ".toRegex(), 3)
        return Request(method, path, protocol)
    }

    /**
     * Reads the headers from the request stream.
     *
     * Example:
     *   Authorization: Bearer AF79E12_34
     *   Content-Length: 12
     *
     * @param input The data source stream to read from.
     * @return The exhausted request headers.
     *
     * @throws IOException if the source can't be read.
     */
    private fun readRequestHeaders(input: BufferedSource): List<String> {
        val headers = mutableListOf<String>()
        var line = input.readUtf8LineStrict()
        while (line.isNotBlank()) {
            headers.add(line)
            line = input.readUtf8LineStrict()
        }
        return headers
    }

    /**
     * Reads the request body from the request stream.
     *
     * @param request The request meta data. Used to determine content length or chunk size.
     * @param input The data source stream to read from.
     * @return A byte buffer containing the exhausted request body.
     *
     * @throws IOException if the source can't be read.
     */
    private fun readRequestContent(request: Request, input: BufferedSource): ByteArray {
        return when {
            request.headers.expectSolidContent -> input.readByteArray()
            request.headers.expectChunkedContent -> {
                val buffer = Buffer()
                var chunkSize = input.readUtf8LineStrict().toLong(16)
                while (chunkSize != 0L) {
                    buffer.writeUtf8(chunkSize.toString()).writeUtf8("\r\n")
                    input.read(buffer, chunkSize)
                    chunkSize = input.readUtf8LineStrict().toLong(16)
                }
                buffer.readByteArray()
            }
            else -> byteArrayOf()
        }
    }

    /**
     * Ensures that there is a Content-Length header when there should be one.
     * Exception is made for chunked content, where no header will be added.
     * Furthermore, an already existing header will NOT be overwritten.
     *
     * @param response The response holding the headers to validate
     */
    private fun ensureContentLength(response: Response) {
        response.headers
            .takeIf { !it.expectSolidContent } // Content-Length already defined
            ?.takeIf { !it.expectChunkedContent }
            ?.takeIf { response.content.isNotEmpty() }
            ?.also { it.contentLength = response.content.size }
    }

    /**
     * Ensures that there is a Sec-WebSocket-Accept header in the response.
     * The header is calculated based on the given Sec-WebSocket-Key header.
     * Does nothing if the key is null or blank.
     */
    private fun ensureSecWebSocketAccept(webSocketKey: String?, response: Response) {
        webSocketKey
            ?.takeIf { it.isNotBlank() }
            ?.let { it + WS_ACCEPT_UUID }
            ?.let { MessageDigest.getInstance("SHA-1").digest(it.toByteArray(UTF_8)) }
            ?.let { Base64.getEncoder().encode(it).toString(UTF_8) }
            ?.also { response.headers.webSocketAccept = it }
    }

    /**
     * Writes the response signature line to the response stream.
     *
     * @param response The mocked response to deduct the signature from.
     * @param output The target stream to write data to.
     *
     * @throws IOException if the target can't be written.
     */
    private fun writeResponseSignature(response: Response, output: BufferedSink) {
        val signature = "${response.protocol} ${response.code} ${response.phrase}"
            .replace("\\r".toRegex(), " ")
            .replace("\\n".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
        output.writeUtf8("$signature\r\n")
    }

    /**
     * Writes the response headers to the response stream.
     *
     * @param response The mocked response containing the headers.
     * @param output The target stream to write to.
     *
     * @throws IOException if the target can't be written.
     */
    private fun writeResponseHeaders(response: Response, output: BufferedSink) {
        response.headers
            .joinToString(separator = "\r\n", postfix = "\r\n") { it.trim() }
            .also { output.writeUtf8(it) }
    }

    /**
     * Writes the response body to the response stream.
     *
     * @param response The mocked response to draw the body from.
     * @param output The target stream to write to.
     *
     * @throws IOException if the target can't be written.
     */
    private fun writeResponseContent(response: Response, output: BufferedSink) {
        var bytes = response.body
        output.writeUtf8("\r\n")
        while (bytes != null) {
            output.write(bytes)
            bytes = response.body
        }
        output.flush()
    }

    /**
     * Default WebSocket callback implementation.
     *
     * @param path The source of the web socket message.
     * @param message The message raw data.
     * @param isText Whether the message is text or binary data.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun handleWebSocketMessage(path: String, message: ByteArray, isText: Boolean) {
    }
}
