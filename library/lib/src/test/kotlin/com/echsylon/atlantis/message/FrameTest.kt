package com.echsylon.atlantis.message

import okio.buffer
import okio.source
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class FrameTest {

    @Test
    fun `When creating close frame, the control bit field is correctly reflected in state`() {
        val frame = Frame(0x88)
        frame.isFinalFrame `should be equal to` true
        frame.isCloseFrame `should be equal to` true
        frame.isContinuationFrame `should be equal to` false
        frame.isBinaryFrame `should be equal to` false
        frame.isTextFrame `should be equal to` false
        frame.isPingFrame `should be equal to` false
        frame.isPongFrame `should be equal to` false
    }

    @Test
    fun `When creating ping frame, the control bit field is correctly reflected in state`() {
        val frame = Frame(0x89)
        frame.isFinalFrame `should be equal to` true
        frame.isCloseFrame `should be equal to` false
        frame.isContinuationFrame `should be equal to` false
        frame.isBinaryFrame `should be equal to` false
        frame.isTextFrame `should be equal to` false
        frame.isPingFrame `should be equal to` true
        frame.isPongFrame `should be equal to` false
    }

    @Test
    fun `When creating pong frame, the control bit field is correctly reflected in state`() {
        val frame = Frame(0x8A)
        frame.isFinalFrame `should be equal to` true
        frame.isCloseFrame `should be equal to` false
        frame.isContinuationFrame `should be equal to` false
        frame.isBinaryFrame `should be equal to` false
        frame.isTextFrame `should be equal to` false
        frame.isPingFrame `should be equal to` false
        frame.isPongFrame `should be equal to` true
    }

    @Test
    fun `When creating text frame, the control bit field is correctly reflected in state`() {
        val frame = Frame(0x81)
        frame.isFinalFrame `should be equal to` true
        frame.isCloseFrame `should be equal to` false
        frame.isContinuationFrame `should be equal to` false
        frame.isBinaryFrame `should be equal to` false
        frame.isTextFrame `should be equal to` true
        frame.isPingFrame `should be equal to` false
        frame.isPongFrame `should be equal to` false
    }

    @Test
    fun `When creating data frame, the control bit field is correctly reflected in state`() {
        val frame = Frame(0x82)
        frame.isFinalFrame `should be equal to` true
        frame.isCloseFrame `should be equal to` false
        frame.isContinuationFrame `should be equal to` false
        frame.isBinaryFrame `should be equal to` true
        frame.isTextFrame `should be equal to` false
        frame.isPingFrame `should be equal to` false
        frame.isPongFrame `should be equal to` false
    }
}
