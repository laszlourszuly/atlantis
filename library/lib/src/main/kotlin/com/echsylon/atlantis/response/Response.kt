package com.echsylon.atlantis.response

import com.echsylon.atlantis.Order
import com.echsylon.atlantis.Order.SEQUENTIAL
import com.echsylon.atlantis.extension.connection
import com.echsylon.atlantis.extension.expectChunkedContent
import com.echsylon.atlantis.extension.upgrade
import okio.Buffer

/**
 * Describes a mock response to serve for an intercepted client request.
 *
 * @param code     The mock response code.
 * @param protocol The mock response protocol and version.
 */
data class Response(
    var code: Int = 200,
    var protocol: String = "HTTP/1.1",
    val headers: ArrayList<String> = arrayListOf(),
    var messageOrder: Order = SEQUENTIAL,
    val messages: ArrayList<Message> = arrayListOf(),
    val behavior: Behavior = Behavior(),
) {
    // Helps us deduct an HTTP status phrase
    private val status = StatusHelper()
    private var payload = PayloadHelper()

    /**
     * The content to serve for this response. This differs from the body
     * field in that this is the full content for the response.
     */
    var content: ByteArray
        get() = payload.template
        set(value) {
            payload.template = value
        }

    /**
     * The next chunk of body content to serve. This may be the full content,
     * or a part of it, all depending on how the behavior configuration is set.
     */
    val body: ByteArray?
        get() = payload.getBlocking(delay, chunk, headers.expectChunkedContent)

    /**
     * The optional delay range before serving the response to the client.
     */
    var delay: IntRange
        get() = behavior.delay
        set(value) = run { behavior.delay = value }

    /**
     * The optional delay fragmentation range when serving the response body.
     */
    var chunk: IntRange
        get() = behavior.chunk
        set(value) = run { behavior.chunk = value }

    /**
     * The mock response status message. This is rendered based on the status
     * code.
     */
    val phrase: String
        get() = status.getPhrase(code)

    /**
     * Whether this seems to be a web socket handshake response.
     */
    val isWebSocket: Boolean
        get() = code == 101 &&
                headers.upgrade?.equals("websocket", ignoreCase = true) ?: false &&
                headers.connection?.equals("upgrade", ignoreCase = true) ?: false

    /**
     * Whether this response has WebSocket messaging capabilities configured.
     */
    val hasMessages: Boolean
        get() = messages.isNotEmpty()

    /**
     * Prints a pleasant and human-readable string representation of the
     * mock response.
     */
    override fun toString(): String {
        return "$protocol $code $phrase\n\t${headers.joinToString("\n\t")}\n\t${Buffer().write(payload.template)}"
    }
}
