package com.echsylon.atlantis.request

import com.echsylon.atlantis.extension.connection
import com.echsylon.atlantis.extension.upgrade
import com.echsylon.atlantis.extension.webSocketKey
import okio.Buffer

/**
 * Describes an intercepted HTTP request from the client.
 *
 * @param verb     The request method.
 * @param path     The full path to the requested resource.
 * @param protocol The request protocol and version.
 */
data class Request(
    val verb: String = "GET",
    val path: String = "/",
    val protocol: String = "HTTP/1.1"
) {
    /**
     * Holds all successfully parsed client request headers.
     */
    val headers: ArrayList<String> = arrayListOf()

    /**
     * Holds the successfully read client request body. May be empty.
     */
    var content: ByteArray = byteArrayOf()

    /**
     * Whether this seems to be a web socket handshake request or not.
     */
    val isWebSocket: Boolean
        get() = verb.equals("get", ignoreCase = true) &&
                headers.webSocketKey?.isNotBlank() ?: false &&
                headers.upgrade?.equals("websocket", ignoreCase = true) ?: false &&
                headers.connection?.equals("upgrade", ignoreCase = true) ?: false

    /**
     * Prints a pleasant and human-readable string representation of the
     * mock response.
     */
    override fun toString(): String {
        return "$protocol $verb $path\n\t${headers.joinToString("\n\t")}\n\t${Buffer().write(content)}"
    }
}
