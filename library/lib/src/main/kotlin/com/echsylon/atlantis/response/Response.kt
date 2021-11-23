package com.echsylon.atlantis.response

import com.echsylon.atlantis.extension.isZero
import com.echsylon.atlantis.extension.toHexString
import com.echsylon.atlantis.header.Headers
import kotlin.math.max
import kotlin.math.min
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.lang.Thread.sleep

/**
 * Describes a mock response to serve for an intercepted client request.
 *
 * @param code     The mock response code.
 * @param protocol The mock response protocol and version.
 */
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

    /**
     * The rendered mock response body.
     */
    var content: ByteArray = byteArrayOf()

    /**
     * The configured mock response headers.
     */
    // FIXME: Maybe move this to the class signature.
    val headers: Headers = Headers()

    /**
     * The configured serve behavior.
     */
    // FIXME: Maybe move this to the class signature.
    val behavior: Behavior = Behavior()

    /**
     * The chunk of body content serve right now. This may be the entired
     * configured body content, or a part of it, all depending on how the
     * behavior configuration is set.
     */
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

    /**
     * The mock response status message. This is rendered based on the status
     * code.
     */
    val phrase: String
        get() = status.getPhrase(code)

    /**
     * Prints a pleasant and human readable string representation of the
     * mock response.
     */
    override fun toString(): String {
        return "$protocol $code $phrase\n\t${headers.joinToString("\n\t")}\n\t${Buffer().write(content)}"
    }

    /**
     * Get the next part of the request body as defined in the corresponding
     * behavior properties. This function may block the calling thread.
     */
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

    /**
     * Get the next part of the request body as a "chunked" transfer encoded
     * block. The chunk size is determined by the corresponding behvior
     * definition. This function may block the calling thread.
     */
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

    /**
     * Determines the number of bytes to serve from the mock response body.
     *
     * @param range The configured "chunk" size range.
     * @param max   The remaining, not-yet-served byte size.
     */
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

    /**
     * Determines the actual duration to delay the chunk of data currently
     * being served.
     *
     * @param range The configured delay range.
     */
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
