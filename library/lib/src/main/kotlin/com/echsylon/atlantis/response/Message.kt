package com.echsylon.atlantis.response

import com.echsylon.atlantis.message.Type
import okio.Buffer

@Suppress("ArrayInDataClass")
data class Message(
    var path: String = "",
    var text: String? = null,
    var data: ByteArray? = null,
    var type: Type? = null,
    var code: Int? = null,
    var delay: IntRange = 0..0,
    var chunk: IntRange = 0..0
) {
    val payload: ByteArray?
        get() = text?.encodeToByteArray() ?: data

    override fun toString(): String {
        return text
            ?: data?.let { Buffer().write(it).toString() }
            ?: ""
    }
}

