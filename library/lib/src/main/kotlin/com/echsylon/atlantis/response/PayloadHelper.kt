package com.echsylon.atlantis.response

import com.echsylon.atlantis.extension.isZero
import com.echsylon.atlantis.extension.toHexString
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import okio.Buffer

/**
 * This class offers means of delayed reading parts of a template byte array.
 * The size and delay of each part is randomized between the given ranges.
 */
class PayloadHelper {
    internal var template: ByteArray = byteArrayOf()
        set(value) {
            field = value
            payloadCache = Buffer().write(value)
        }

    private var payloadCache: Buffer = Buffer().write(template)

    /**
     * Get the next random sized part of the payload formatted as defined by
     * the formatResultAsChunkedData constructor flag. This function may block
     * for a random amount of milliseconds.
     *
     * @param delayHint The random delay range.
     * @param lengthHint The random part size range.
     * @param formatResultAsChunkedData Whether to return chunk-formatted
     *                                  data or not.
     *
     * @return The unformatted next part of the payload, or null once if the
     * payload has been fully read. NOTE! In the latter case, this function
     * will re-populate the internal payload buffer before returning null. This
     * means that next time this function is called, it may very well return a
     * non-null result.
     */
    fun getBlocking(
        delayHint: IntRange = 0..0,
        lengthHint: IntRange = 0..0,
        formatResultAsChunkedData: Boolean = false
    ): ByteArray? {
        return when (formatResultAsChunkedData) {
            true -> getChunkedBlocking(delayHint, lengthHint)
            else -> getSolidBlocking(delayHint, lengthHint)
        }
    }

    /**
     * Tries to get the next random sized part of the payload "as is", that is,
     * with no further protocol formatting applied. This function may block for
     * a random amount of milliseconds.
     *
     * @param delayHint The random delay range.
     * @param lengthHint The random part size range.
     *
     * @return The unformatted next part of the payload, or null once if the
     * payload has been fully read. NOTE! In the latter case, this function
     * will re-populate the internal payload buffer before returning null.
     * This means that the next time this function is called, it may very well
     * return a non-null result.
     */
    fun getSolidBlocking(
        delayHint: IntRange = 0..0,
        lengthHint: IntRange = 0..0
    ): ByteArray? {
        return if (payloadCache.size == 0L) {
            payloadCache = Buffer().write(template)
            null
        } else {
            runCatching {
                Thread.sleep(getWaitTime(delayHint))
                val count = getReadSize(lengthHint)
                payloadCache.readByteArray(count)
            }.getOrNull()
        }
    }

    /**
     * Tries to get the next random sized part of the payload and wrap it with
     * the chunk meta-data. This function may block for a random amount of
     * milliseconds.
     *
     * @param delayHint The random delay range.
     * @param lengthHint The random part size range.
     *
     * @return The next chunk-formatted part of the payload, or null once if
     * the payload has been fully read. NOTE! In the latter case, this function
     * will re-populate the internal payload buffer before returning null. This
     * means that next time this function is called it may very well return a
     * non-null result.
     */
    fun getChunkedBlocking(
        delayHint: IntRange = 0..0,
        lengthHint: IntRange = 0..0
    ): ByteArray? {
        return if (payloadCache.size == 0L) {
            payloadCache = Buffer().write(template)
            null
        } else {
            runCatching {
                Thread.sleep(getWaitTime(delayHint))
                val count = getReadSize(lengthHint)
                ByteArrayOutputStream()
                    .apply { write("${count.toHexString()}\r\n".encodeToByteArray()) }
                    .apply { write(payloadCache.readByteArray(count)) }
                    .toByteArray()
            }.getOrNull()
        }
    }


    /**
     * Determines the number of bytes to serve from the mock response body.
     *
     * @param range The desired fragment size range.
     */
    private fun getReadSize(range: IntRange?): Long {
        val size = payloadCache.size
        return when {
            range == null -> size
            range.isZero() -> size
            range.isEmpty() -> size
            range.first > size -> size
            else -> LongRange(max(0L, range.first.toLong()), min(size, range.last.toLong())).random()
        }
    }

    /**
     * Determines the actual duration to delay the chunk of data currently
     * being served.
     *
     * @param range The desired delay range.
     */
    private fun getWaitTime(range: IntRange?): Long {
        return when {
            range == null -> 0L
            range.isZero() -> 0L
            range.isEmpty() -> 0L
            else -> LongRange(max(0L, range.first.toLong()), range.last.toLong()).random()
        }
    }
}
