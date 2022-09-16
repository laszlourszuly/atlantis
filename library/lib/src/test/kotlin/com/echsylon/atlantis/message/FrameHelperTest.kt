package com.echsylon.atlantis.message

import com.echsylon.atlantis.message.Reason.NORMAL_CLOSURE
import com.echsylon.atlantis.message.exception.PayloadTooLargeException
import com.echsylon.atlantis.message.exception.UnmaskedFrameException
import okio.buffer
import okio.sink
import okio.source
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.shouldContainSame
import org.junit.Test
import java.io.ByteArrayOutputStream

class FrameHelperTest {
    @Test
    fun `When reading frame with MASK bit set, then isFrameMasked returns true`() {
        val stream = byteArrayOf(
            0x82.toByte(), // FIN BIT (true) + OPCODE (0x02 -> binary frame)
            0x80.toByte(), // MASK BIT (true) + PAYLOAD SIZE (value: 0x00 -> this is size)
            0x23.toByte(), // MASK 1
            0x24.toByte(), // MASK 2
            0x25.toByte(), // MASK 3
            0x26.toByte()  // MASK 4
        )
        val helper = FrameHelper()
        val frame = helper.readFrame(stream.inputStream().source().buffer())
        frame.isMaskedFrame `should be equal to` true
    }

    @Test
    fun `When reading frame with PAYLOAD_LEN less than 126, then payloadSize returns that value`() {
        val stream = byteArrayOf(
            0x82.toByte(), // FIN BIT (true) + OPCODE (0x02 -> binary frame)
            0x81.toByte(), // MASK BIT (true) + PAYLOAD SIZE (value: 0x01 -> this is size)
            0x23.toByte(), // MASK 1
            0x24.toByte(), // MASK 2
            0x25.toByte(), // MASK 3
            0x26.toByte(), // MASK 4
            0xFF.toByte()  // PAYLOAD 1
        )
        val helper = FrameHelper()
        val frame = helper.readFrame(stream.inputStream().source().buffer())
        frame.payload?.size `should be equal to` 1
    }

    @Test
    fun `When reading frame with PAYLOAD_LEN equal to 126, then payload size is read from next two bytes`() {
        val stream = byteArrayOf(
            0x82.toByte(), // FIN BIT (true) + OPCODE (0x02 -> binary frame)
            0xFE.toByte(), // MASK BIT (true) + PAYLOAD SIZE (value: 0x7E -> size is next 2 bytes)
            0x00.toByte(), // PAYLOAD SIZE 1
            0x01.toByte(), // PAYLOAD SIZE 2
            0x23.toByte(), // MASK 1
            0x24.toByte(), // MASK 2
            0x25.toByte(), // MASK 3
            0x26.toByte(), // MASK 4
            0x77.toByte()  // PAYLOAD 1
        )
        val helper = FrameHelper()
        val frame = helper.readFrame(stream.inputStream().source().buffer())
        frame.payload?.size `should be equal to` 1
    }

    @Test
    fun `When reading frame with PAYLOAD_LEN greater than 126, then this implementation will throw exception`() {
        val stream = byteArrayOf(
            0x82.toByte(), // FIN BIT (true) + OPCODE (0x02 -> binary frame)
            0xFF.toByte(), // MASK BIT (true) + PAYLOAD SIZE (value: 0x7F -> size is next 8 bytes)
            0xF0.toByte(), // PAYLOAD SIZE 1
            0x00.toByte(), // PAYLOAD SIZE 2
            0x00.toByte(), // PAYLOAD SIZE 3
            0x00.toByte(), // PAYLOAD SIZE 4
            0x00.toByte(), // PAYLOAD SIZE 5
            0x00.toByte(), // PAYLOAD SIZE 6
            0x00.toByte(), // PAYLOAD SIZE 7
            0x01.toByte(), // PAYLOAD SIZE 8
            0x23.toByte(), // MASK 1
            0x24.toByte(), // MASK 2
            0x25.toByte(), // MASK 3
            0x26.toByte(), // MASK 4
            0x77.toByte()  // PAYLOAD 1
        )
        val helper = FrameHelper()
        invoking { helper.readFrame(stream.inputStream().source().buffer()) } `should throw` PayloadTooLargeException::class
    }

    @Test
    fun `When reading frame with MASK bit set, the parsed data is correctly unmasked`() {
        val stream = byteArrayOf(
            0x82.toByte(), // FIN BIT (true) + OPCODE (0x02 -> binary data)
            0x85.toByte(), // MASK BIT (true) + PAYLOAD SIZE (value: 0x05 -> this is size)
            0x23.toByte(), // MASK 1
            0x24.toByte(), // MASK 2
            0x25.toByte(), // MASK 3
            0x26.toByte(), // MASK 4
            0x77.toByte(), // MASKED PAYLOAD 1
            0x77.toByte(), // MASKED PAYLOAD 2
            0x77.toByte(), // MASKED PAYLOAD 3
            0x77.toByte(), // MASKED PAYLOAD 4
            0x77.toByte()  // MASKED PAYLOAD 5
        )
        val helper = FrameHelper()
        val frame = helper.readFrame(stream.inputStream().source().buffer())
        frame.payload!! shouldContainSame byteArrayOf(0x54.toByte(), 0x53.toByte(), 0x52.toByte(), 0x51.toByte(), 0x54.toByte())
    }

    @Test
    fun `When reading frame with MASK bit not set, an exception is thrown`() {
        val stream = byteArrayOf(
            0x82.toByte(), // FIN BIT (true) + OPCODE (0x02 -> binary data)
            0x05.toByte(), // MASK BIT (false) + PAYLOAD SIZE (value: 0x05 -> this is size)
            0x77.toByte(), // PAYLOAD 1
            0x77.toByte(), // PAYLOAD 2
            0x77.toByte(), // PAYLOAD 3
            0x77.toByte(), // PAYLOAD 4
            0x77.toByte()  // PAYLOAD 5
        )
        val helper = FrameHelper()
        invoking { helper.readFrame(stream.inputStream().source().buffer()) } `should throw` UnmaskedFrameException::class
    }

    @Test
    fun `When creating close frame, the MASK bit is not set`() {
        val frame = FrameHelper().createCloseFrame(NORMAL_CLOSURE)
        frame.isMaskedFrame `should be equal to` false
    }

    @Test
    fun `When creating ping frame, the MASK bit is not set`() {
        val frame = FrameHelper().createPingFrame()
        frame.isMaskedFrame `should be equal to` false
    }

    @Test
    fun `When creating pong frame, the MASK bit is not set`() {
        val frame = FrameHelper().createPongFrame()
        frame.isMaskedFrame `should be equal to` false
    }

    @Test
    fun `When creating text frame, the MASK bit is not set`() {
        val frame = FrameHelper().createTextFrames("hello").first()
        frame.isMaskedFrame `should be equal to` false
    }

    @Test
    fun `When creating data frame, the MASK bit is not set`() {
        val frame = FrameHelper().createDataFrames(byteArrayOf(0x01, 0x01, 0x01)).first()
        frame.isMaskedFrame `should be equal to` false
    }

    @Test
    fun `When creating close frame with payload bigger than 125 bytes, only the first 125 bytes are included`() {
        val payload = "A".repeat(126)
        val frame = FrameHelper().createCloseFrame(NORMAL_CLOSURE, payload)
        frame.payload?.size `should be equal to` 125
    }

    @Test
    fun `When creating ping frame with payload bigger than 125 bytes, only the first 125 bytes are included`() {
        val payload = ByteArray(126) { 0x01 }
        val frame = FrameHelper().createPingFrame(payload)
        frame.payload?.size `should be equal to` 125
    }

    @Test
    fun `When creating pong frame with payload bigger than 125 bytes, only the first 125 bytes are included`() {
        val payload = ByteArray(126) { 0x02 }
        val frame = FrameHelper().createPongFrame(payload)
        frame.payload?.size `should be equal to` 125
    }

    @Test
    fun `When creating text frame with payload bigger than frame size, multiple frames are produced`() {
        val frames = FrameHelper().createTextFrames("ABCD", 2)
        frames.size `should be equal to` 2
    }

    @Test
    fun `When creating data frame with payload bigger than frame size, multiple frames are produced`() {
        val frames = FrameHelper().createDataFrames(byteArrayOf(0x01, 0x02, 0x03, 0x04), 2)
        frames.size `should be equal to` 2
    }

    @Test
    fun `When creating close frame, the control bit field is correct`() {
        val frame = FrameHelper().createCloseFrame(NORMAL_CLOSURE)
        frame.control `should be equal to` 0x88
    }

    @Test
    fun `When creating ping frame, the control bit field is correct`() {
        val frame = FrameHelper().createPingFrame()
        frame.control `should be equal to` 0x89
    }

    @Test
    fun `When creating pong frame, the control bit field is correct`() {
        val frame = FrameHelper().createPongFrame()
        frame.control `should be equal to` 0x8A
    }

    @Test
    fun `When creating a single text frame, the control bit field is correct`() {
        val frame = FrameHelper().createTextFrames("hello").first()
        frame.control `should be equal to` 0x81
    }

    @Test
    fun `When creating a split text frame, the control bit fields are correct`() {
        val frames = FrameHelper().createTextFrames("hello", 2)
        frames.size `should be equal to` 3
        frames[0].control `should be equal to` 0x01
        frames[1].control `should be equal to` 0x00
        frames[2].control `should be equal to` 0x80
    }

    @Test
    fun `When creating a single data frame, the control bit field is correct`() {
        val frame = FrameHelper().createDataFrames(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)).first()
        frame.control `should be equal to` 0x82
    }

    @Test
    fun `When creating a split data frame, the control bit fields are correct`() {
        val frames = FrameHelper().createDataFrames(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05), 2)
        frames.size `should be equal to` 3
        frames[0].control `should be equal to` 0x02
        frames[1].control `should be equal to` 0x00
        frames[2].control `should be equal to` 0x80
    }

    @Test
    fun `When creating a split text frame, the data is fragmented correctly`() {
        val frames = FrameHelper().createTextFrames("abc", 2)
        frames.size `should be equal to` 2
        frames[0].payload!!.decodeToString() `should be equal to` "ab"
        frames[1].payload!!.decodeToString() `should be equal to` "c"
    }

    @Test
    fun `When creating a split data frame, the data is fragmented correctly`() {
        val frames = FrameHelper().createDataFrames(byteArrayOf(0x01, 0x02, 0x03), 2)
        frames.size `should be equal to` 2
        frames[0].payload!! `should be equal to` byteArrayOf(0x01, 0x02)
        frames[1].payload!! `should be equal to` byteArrayOf(0x03)
    }

    @Test
    fun `When writing a close frame, the correct output stream is produced`() {
        val frameHelper = FrameHelper()
        val frame = frameHelper.createCloseFrame(NORMAL_CLOSURE)
        val output = ByteArrayOutputStream(4)
        frameHelper.writeFrame(frame, output.sink().buffer())
        output.toByteArray() `should be equal to` byteArrayOf(0x88.toByte(), 0x02.toByte(), 0x03.toByte(), 0xE8.toByte())
    }

    @Test
    fun `When writing an empty ping frame, the correct output stream is produced`() {
        val frameHelper = FrameHelper()
        val frame = frameHelper.createPingFrame()
        val output = ByteArrayOutputStream(2)
        frameHelper.writeFrame(frame, output.sink().buffer())
        output.toByteArray() `should be equal to` byteArrayOf(0x89.toByte(), 0x00.toByte())
    }

    @Test
    fun `When writing a ping frame with data, the correct output stream is produced`() {
        val frameHelper = FrameHelper()
        val frame = frameHelper.createPingFrame(byteArrayOf(0x7F))
        val output = ByteArrayOutputStream(3)
        frameHelper.writeFrame(frame, output.sink().buffer())
        output.toByteArray() `should be equal to` byteArrayOf(0x89.toByte(), 0x01.toByte(), 0x7F.toByte())
    }

    @Test
    fun `When writing an empty pong frame, the correct output stream is produced`() {
        val frameHelper = FrameHelper()
        val frame = frameHelper.createPongFrame()
        val output = ByteArrayOutputStream(2)
        frameHelper.writeFrame(frame, output.sink().buffer())
        output.toByteArray() `should be equal to` byteArrayOf(0x8A.toByte(), 0x00.toByte())
    }

    @Test
    fun `When writing a pong frame with data, the correct output stream is produced`() {
        val frameHelper = FrameHelper()
        val frame = frameHelper.createPongFrame(byteArrayOf(0x7F))
        val output = ByteArrayOutputStream(3)
        frameHelper.writeFrame(frame, output.sink().buffer())
        output.toByteArray() `should be equal to` byteArrayOf(0x8A.toByte(), 0x01.toByte(), 0x7F.toByte())
    }

    @Test
    fun `When writing a text frame, the correct output stream is produced`() {
        val frameHelper = FrameHelper()
        val frame = frameHelper.createTextFrames("a").first()
        val output = ByteArrayOutputStream(3)
        frameHelper.writeFrame(frame, output.sink().buffer())
        output.toByteArray() `should be equal to` byteArrayOf(0x81.toByte(), 0x01.toByte(), 0x61.toByte())
    }

    @Test
    fun `When writing a data frame, the correct output stream is produced`() {
        val frameHelper = FrameHelper()
        val frame = frameHelper.createDataFrames(byteArrayOf(0x7E, 0x7C)).first()
        val output = ByteArrayOutputStream(3)
        frameHelper.writeFrame(frame, output.sink().buffer())
        output.toByteArray() `should be equal to` byteArrayOf(0x82.toByte(), 0x02.toByte(), 0x7E.toByte(), 0x7C.toByte())
    }
}
