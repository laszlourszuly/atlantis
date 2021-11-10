package com.echsylon.atlantis.response

import com.echsylon.atlantis.extension.isZero
import com.echsylon.atlantis.extension.toHexString
import com.echsylon.atlantis.header.Headers
import kotlin.math.max
import kotlin.math.min
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.lang.Thread.sleep

data class Response(
    var code: Int = 200,
    var protocol: String = "HTTP/1.1",
) {
    // Helps us deduct a HTTP status phrase
    private val status = StatusHelper()

    // Holds the buffer of the currently served response body. Once this
    // response is served, this buffer is re-populated with the content from
    // the initial configuration. This allows us to serve the response multiple
    // times as we're not draining the actual template content, but rather a
    // copy of it.
    private var bytes: Buffer? = null
    var content: ByteArray = byteArrayOf()
    val headers: Headers = Headers()
    val behavior: Behavior = Behavior()

    val body: ByteArray?
        get() = let {
            if (bytes?.size == 0L) {
                bytes = Buffer().write(content)
                return null
            }

            if (bytes == null) bytes = Buffer().write(content)
            if (headers.expectChunkedContent) getChunkedDelayed()
            else getDelayed()
        }

    val phrase: String
        get() = status.getPhrase(code)

    override fun toString(): String {
        return "$protocol $code $phrase\n\t${headers.joinToString("\n\t")}\n\t${Buffer().write(content)}"
    }

    private fun getDelayed(): ByteArray? {
        return bytes
            ?.takeIf { it.size > 0 }
            ?.runCatching {
                val chunk = behavior.chunk
                val delay = behavior.delay
                val count = getReadSize(chunk, size.toULong())
                sleep(getWaitTime(delay))
                readByteArray(count)
            }
            ?.getOrNull()
    }

    private fun getChunkedDelayed(): ByteArray? {
        return bytes
            ?.takeIf { it.size > 0 }
            ?.runCatching {
                val chunk = behavior.chunk
                val delay = behavior.delay
                val count = getReadSize(chunk, size.toULong())
                ByteArrayOutputStream()
                    .apply { write("${count.toHexString()}\r\n".encodeToByteArray()) }
                    .apply { write(readByteArray(count)) }
                    .apply { sleep(getWaitTime(delay)) }
                    .toByteArray()
            }
            ?.getOrNull()
    }

    private fun getReadSize(range: ULongRange?, max: ULong): Long {
        val unsigned: ULong = when {
            range == null -> min(1024UL, max)
            range.isZero() -> min(1024UL, max)
            range.isEmpty() -> min(1024UL, max)
            range.first > max -> min(1024UL, max)
            range.last > max -> ULongRange(range.first, max(range.first, max)).random()
            else -> range.random()
        }

        // Make sure the unsigned -> signed conversion is
        // capped on the positive spectra of Long values.
        val candidate = unsigned.toLong()
        return if (candidate < 0L) Long.MAX_VALUE else candidate
    }

    private fun getWaitTime(range: ULongRange?): Long {
        val unsigned: ULong = when {
            range == null -> 0UL
            range.isZero() -> 0UL
            range.isEmpty() -> 0UL
            else -> range.random()
        }

        // Make sure the unsigned -> signed conversion is
        // capped on the positive spectra of Long values.
        val candidate = unsigned.toLong()
        return if (candidate < 0L) Long.MAX_VALUE else candidate
    }
}
