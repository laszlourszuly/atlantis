package com.echsylon.atlantis.request

import com.echsylon.atlantis.header.Headers
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
     * Holds all successfully paresed client request headers.
     */
    val headers: Headers = Headers()

    /**
     * Holds the successfully read client request body. May be empty.
     */
    var content: ByteArray = byteArrayOf()

    /**
     * Prints a pleasant and human readable string representation of the
     * intercepted client request.
     */
    override fun toString(): String {
        return "$protocol $verb $path\n\t${headers.joinToString("\n\t")}\n\t${Buffer().write(content)}"
    }
}
