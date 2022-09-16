package com.echsylon.atlantis.message

import okio.Buffer

/**
 * Holds the WebSocket frame data. Note that this is not the full message, but
 * rather a fragmented data frame.
 */
@Suppress("ArrayInDataClass") // This class is not supposed to be compared
data class Frame(
    val control: Int = 0x00,
    val mask: ByteArray? = null,
    val payload: ByteArray? = null
) {
    companion object {
        private const val FIN_MASK = 0x80
        private const val RSV_MASK = 0x70
        private const val OPC_MASK = 0x0F
    }

    val isContinuationFrame: Boolean
        get() = control and OPC_MASK == 0x00

    val isBinaryFrame: Boolean
        get() = control and OPC_MASK == 0x02

    val isTextFrame: Boolean
        get() = control and OPC_MASK == 0x01

    val isCloseFrame: Boolean
        get() = control and OPC_MASK == 0x08

    val isPingFrame: Boolean
        get() = control and OPC_MASK == 0x09

    val isPongFrame: Boolean
        get() = control and OPC_MASK == 0x0A

    val isMaskedFrame: Boolean
        get() = mask != null

    val isFinalFrame: Boolean
        get() = control and FIN_MASK != 0

    val isRsv1: Boolean
        get() = control and RSV_MASK == 0x40

    val isRsv2: Boolean
        get() = control and RSV_MASK == 0x20

    val isRsv3: Boolean
        get() = control and RSV_MASK == 0x10

    override fun toString(): String {
        return """
            {
                final: $isFinalFrame
                rsv1: $isRsv1
                rsv2: $isRsv2
                rsv3: $isRsv3
                continuation: $isContinuationFrame,
                text: $isTextFrame
                binary: $isBinaryFrame
                close: $isCloseFrame
                ping: $isPingFrame
                pong: $isPongFrame
                masked: $isMaskedFrame
                length: ${payload?.size ?: 0}
                payload: ${payload?.let { Buffer().write(it) }}
            }""".trimIndent()
    }
}
