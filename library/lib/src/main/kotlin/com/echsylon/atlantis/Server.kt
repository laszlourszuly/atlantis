package com.echsylon.atlantis

import com.echsylon.atlantis.extension.closeSilently
import com.echsylon.atlantis.request.Request
import com.echsylon.atlantis.response.Response
import kotlin.text.split
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.net.ServerSocketFactory
import kotlin.IllegalArgumentException
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * This class represents the remote server and is the heart of Atlantis.
 * It listens for HTTP requests and serves corresponding HTTP responses
 * as defined in the current configuration. The default configuration
 * for this server will serve 404 Not found on all requests.
 */
internal class Server {
    companion object {
        internal val NOT_FOUND = Response(404)
    }

    private var server: ServerSocket? = null
    private var config: Configuration = Configuration()
    private var executor: ExecutorService = Executors.newCachedThreadPool {
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
     * @param port The corresponding port.
     * @return True if the server was successfully started, else false.
     */
    fun start(port: Int) {
        if (isRunning) return
        server = runCatching { createServerSocket(port) }
            .onFailure { it.printStackTrace() }
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
        executor.shutdown()
        server?.closeSilently()
        server = null
    }

    /**
     * Creates a new server socket and tries to bind it to the given address
     * and port.
     *
     * @param port The port to serve mock data on.
     * @return The bound server socket.
     *
     * @throws RuntimeException if anything goes wrong.
     */
    private fun createServerSocket(port: Int): ServerSocket {
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
     * Waits for a client to request a connection to the server socket and
     * serves a mocked response for any read requests.
     *
     * @param socket The socket to wait for clients on.
     *
     * @throws RuntimeException if anything goes wrong.
     */
    private fun serveConnection(socket: Socket) {
        executor.execute {
            lateinit var target: BufferedSink
            lateinit var source: BufferedSource
            runCatching {
                target = socket.sink().buffer()
                source = socket.source().buffer()

                val request = readRequestSignature(source)
                request.headers.addAll(readRequestHeaders(source))
                request.content = readRequestContent(request, source)
                println("Atlantis Request:\n\t$request")

                val response = config.findResponse(request) ?: NOT_FOUND
                if (response.behavior.calculateLength) ensureContentLength(response)
                println("Atlantis Response:\n\t$response")

                writeResponseSignature(response, target)
                writeResponseHeaders(response, target)
                writeResponseContent(response, target)
            }.onSuccess {
                socket.closeSilently()
            }.onFailure { error ->
                error.printStackTrace()
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
            request.headers.expectSolidContent ->
                input.readByteArray()
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
     * Furthermore an already existing header will NOT be overwritten.
     *
     * @param response The response holding the headers to validate
     */
    private fun ensureContentLength(response: Response) {
        if (!response.headers.expectSolidContent && !response.headers.expectChunkedContent) {
            response.headers.add("Content-Length: ${response.content.size}")
        }
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
        if (bytes != null) output.writeUtf8("\r\n")
        while (bytes != null) {
            output.write(bytes)
            bytes = response.body
        }
        output.flush()
    }
}