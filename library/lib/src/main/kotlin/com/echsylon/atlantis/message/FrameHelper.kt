package com.echsylon.atlantis.message

import com.echsylon.atlantis.extension.toUInt
import com.echsylon.atlantis.extension.toULong
import com.echsylon.atlantis.message.Type.CLOSE
import com.echsylon.atlantis.message.Type.DATA
import com.echsylon.atlantis.message.Type.PING
import com.echsylon.atlantis.message.Type.PONG
import com.echsylon.atlantis.message.Type.TEXT
import com.echsylon.atlantis.message.exception.PayloadTooLargeException
import com.echsylon.atlantis.message.exception.UnmaskedFrameException
import okio.BufferedSink
import okio.BufferedSource
import java.io.ByteArrayOutputStream
import kotlin.experimental.xor
import kotlin.math.min

/**
 * This class offers common convenience actions often performed on WebSocket
 * frames and messages.
 */
class FrameHelper {
    private companion object {
        // The maximum allowed payload size of a control frame. Defined in the
        // WebSocket RFC specification.
        private const val MAX_CONTROL_PAYLOAD_SIZE = 125

        // The maximum allowed payload size of a regular content frame. This is
        // a limitation in this particular implementation. The protocol as such
        // allows for max sizes that can be described in 64-bits.
        private const val MAX_CONTENT_PAYLOAD_SIZE = Int.MAX_VALUE
    }

    /**
     * Reads a WebSocket message frame, in a blocking fashion, from the given
     * input stream.
     *
     * @param source The stream to read from.
     * @return The fully read frame.
     * @throws UnmaskedFrameException If the mask bit is not set.
     * @throws PayloadTooLargeException If the payload is larger than
     * MAX_CONTENT_PAYLOAD_SIZE bytes.
     */
    fun readFrame(source: BufferedSource): Frame {
        val control = source.readByte().toInt()
        val length = source.readByte().toInt()

        // Client frames MUST be masked.
        if (length and 0x80 == 0)
            throw UnmaskedFrameException()

        val payloadLength = length and 0x7F
        var payloadSize = 0UL
        if (payloadLength <= 0x7D) payloadSize = payloadLength.toULong()
        if (payloadLength == 0x7E) payloadSize = source.readByteArray(2).toULong()
        if (payloadLength == 0x7F) payloadSize = source.readByteArray(8).toULong()

        if (payloadSize > MAX_CONTENT_PAYLOAD_SIZE.toULong())
            throw PayloadTooLargeException()

        val mask = source.readByteArray(4)
        val payload = source.readByteArray(payloadSize.toLong())
        unmaskPayload(mask, payload)

        return Frame(control, mask, payload)
    }

    /**
     * Writes a WebSocket message frame, in a blocking fashion, to the given
     * output stream.
     *
     * @param frame The frame to serialize and write.
     * @param sink The stream to write to.
     * @throws PayloadTooLargeException If attempting to write a frame that's
     * larger than MAX_CONTENT_PAYLOAD_SIZE bytes.
     */
    fun writeFrame(frame: Frame, sink: BufferedSink) {
        val payload = frame.payload ?: byteArrayOf()
        val size = payload.size

        // Server MUST NOT mask a frame.
        if (size <= 0x7D) {
            sink.writeByte(frame.control)
            sink.writeByte(size and 0x7F)
        } else if (size <= MAX_CONTENT_PAYLOAD_SIZE) {
            sink.writeByte(frame.control)
            sink.writeByte(0x7E)            // size bit
            sink.writeByte(size shr 8)      // size high byte
            sink.writeByte(size and 0xFF)   // size low byte
        } else throw PayloadTooLargeException()

        sink.write(payload)
        sink.flush()
    }

    /**
     * Creates a list of frames with the given frame size carrying the provided
     * text data.
     *
     * @param text The frame(s) payload.
     * @param frameSize The max frame size.
     * @return A list of WebSocket message frames.
     */
    fun createTextFrames(text: String, frameSize: Int = MAX_CONTENT_PAYLOAD_SIZE): List<Frame> {
        val bytes = text.encodeToByteArray()
        return createContentFrames(TEXT, bytes, frameSize)
    }

    /**
     * Creates a list of frames with the given frame size carrying the provided
     * byte array data.
     *
     * @param data The frame(s) payload.
     * @param frameSize The max frame size.
     * @return A list of WebSocket message frames.
     */
    fun createDataFrames(data: ByteArray, frameSize: Int = MAX_CONTENT_PAYLOAD_SIZE): List<Frame> {
        return createContentFrames(DATA, data, frameSize)
    }

    /**
     * Creates a ping frame with an optional payload. NOTE!! that this function
     * will silently cap the payload at MAX_CONTROL_PAYLOAD_SIZE bytes.
     *
     * @param data The optional payload to include.
     * @return The ping frame.
     */
    fun createPingFrame(data: ByteArray? = null): Frame {
        return createControlFrame(PING, data)
    }

    /**
     * Creates a pong frame with an optional payload. NOTE!! that this function
     * will silently cap the payload at MAX_CONTROL_PAYLOAD_SIZE bytes.
     *
     * @param data The optional payload to include.
     * @return The pong frame.
     */
    fun createPongFrame(data: ByteArray? = null): Frame {
        return createControlFrame(PONG, data)
    }

    /**
     * Creates a close frame with an optional close reason and message. NOTE!!
     * that this function will silently cap the message at 123 bytes (2 bytes
     * out of the MAX_CONTROL_PAYLOAD_SIZE are reserved for the reason code).
     *
     * @param reason The optional close reason.
     * @param message The optional message (only included if reason given).
     * @return The close frame.
     */
    fun createCloseFrame(reason: Reason? = null, message: String? = null): Frame {
        return createCloseFrame(reason?.code, message)
    }

    /**
     * Creates a close frame with an optional close reason code and message.
     * NOTE!! that this function will silently cap the message at 123 bytes
     * (2 bytes out of the MAX_CONTROL_PAYLOAD_SIZE are reserved for the reason
     * code).
     *
     * @param reasonCode The optional close reason code.
     * @param message The optional message (only included if reasonCode given).
     * @return The close frame.
     */
    fun createCloseFrame(reasonCode: Int? = null, message: String? = null): Frame {
        val payload = ByteArrayOutputStream()
        reasonCode
            ?.also { payload.write(it shr 8) }
            ?.also { payload.write(it and 0xFF) }
        message
            ?.takeIf { reasonCode != null }
            ?.encodeToByteArray()
            ?.also { payload.write(it) }

        return createControlFrame(CLOSE, payload.toByteArray())
    }

    /**
     * Reads the close reason payload from the frame and returns it as a
     * friendly string.
     *
     * @param frame The frame to read the close reason payload from.
     * @return The close reason as a string, or null if the frame isn't a close
     * frame or if it doesn't have any payload to read.
     */
    fun parseCloseReason(frame: Frame): String? {
        return frame.payload
            ?.takeIf { frame.isCloseFrame }
            ?.takeIf { it.size >= 2 }
            ?.let {
                val code = it.copyOfRange(0, 2).toUInt()
                val msg = it.decodeToString(2)
                "$code $msg"
            }
            ?.trim()
    }

    /**
     * Unmasks the payload with the mask key.
     *
     * @param mask The mask key.
     * @param payload The masked data.
     */
    private fun unmaskPayload(mask: ByteArray? = null, payload: ByteArray? = null) {
        if (mask == null) return
        if (payload == null) return
        payload.forEachIndexed { index, byte ->
            val j = (index % 4)
            val hash = mask[j]
            payload[index] = byte.xor(hash)
        }
    }

    /**
     * Creates a control frame of the requested type with the given payload.
     * This function will silently cap the payload at MAX_CONTROL_PAYLOAD_SIZE
     * bytes if larger.
     *
     * @param type The frame type.
     * @param data The frame payload.
     * @return The composed frame.
     */
    private fun createControlFrame(type: Type, data: ByteArray?): Frame {
        val control = when (type) {
            CLOSE -> 0x88   // FIN + CLOSE
            PING -> 0x89    // FIN + PING
            PONG -> 0x8A    // FIN + PONG
            else -> throw IllegalArgumentException("Unexpected control frame type: $type")
        }

        val payload = data?.let { it.copyOfRange(0, min(it.size, MAX_CONTROL_PAYLOAD_SIZE)) }
        return Frame(control, null, payload)

    }

    /**
     * Creates a list of content frames of the requested type with the given
     * content. The number of frames returned is dictated by the size of the
     * data in combination with the frame size limit.
     *
     * @param type The frame type.
     * @param data The frame(s) payload.
     * @param frameSize The maximum size of payload each frame may take.
     * @return The list of frames. Will always contain at least one frame.
     */
    private fun createContentFrames(type: Type, data: ByteArray?, frameSize: Int): List<Frame> {
        val frames = mutableListOf<Frame>()
        val bytes = data ?: byteArrayOf()
        var skip = 0
        val code = when (type) {
            TEXT -> 0x01
            DATA -> 0x02
            else -> throw IllegalArgumentException("Unexpected content frame type: $type")
        }

        do {
            val payload = ByteArrayOutputStream()
            val size = min(bytes.size - skip, frameSize)
            val first = skip == 0
            val last = skip + size == bytes.size
            val only = first && last
            val control = when {
                only -> code or 0x80    // FIN + TEXT|DATA|PING|PONG
                first -> code           // TEXT|DATA|PING|PONG
                last -> 0x80            // FIN + CNT
                else -> 0x00            // CNT
            }
            payload.write(bytes, skip, size)
            frames.add(Frame(control, null, payload.toByteArray()))
            skip += size
        } while (skip < bytes.size)

        return frames
    }
}
