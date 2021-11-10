package com.echsylon.atlantis.request

import com.echsylon.atlantis.header.Headers
import okio.Buffer

data class Request(
    val verb: String = "GET",
    val path: String = "/",
    val protocol: String = "HTTP/1.1"
) {
    val headers: Headers = Headers()
    var content: ByteArray = byteArrayOf()

    override fun toString(): String {
        return "$protocol $verb $path\n\t${headers.joinToString("\n\t")}\n\t${Buffer().write(content)}"
    }
}
