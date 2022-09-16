package com.echsylon.atlantis.extension

import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.junit.Test

class ByteArrayExtensionTest {
    @Test
    fun `When converting ByteArray of 0xFF to ULong, the result is 255`() {
        val source = byteArrayOf(0xFF.toByte())
        source.toULong() `should be equal to` 255UL
    }

    @Test
    fun `When converting max width ByteArray of 0xFF to ULong, the result is ULong MAX_VALUE`() {
        val source = ByteArray(ULong.SIZE_BYTES) { 0xFF.toByte() }
        source.toULong() `should be equal to` 18446744073709551615UL
    }

    @Test
    fun `When converting empty ByteArray to ULong, the result is 0`() {
        val source = byteArrayOf()
        source.toULong() `should be equal to` 0UL
    }

    @Test
    fun `When converting too wide ByteArray to ULong, an exception is thrown`() {
        val source = ByteArray(ULong.SIZE_BYTES + 1) { 0x00.toByte() }
        invoking { source.toULong() } `should throw` IllegalArgumentException::class
    }

    @Test
    fun `When converting BigEndian ByteArray 0xF000000000000000 to ULong, the result is 1`() {
        val source = ByteArray(ULong.SIZE_BYTES) { index -> if (index == 0) 0x01.toByte() else 0x00.toByte() }
        source.toULong(bigEndian = true) `should be equal to` 1UL
    }

    @Test
    fun `When converting ByteArray of 0xFF to Int, the result is 255`() {
        val source = byteArrayOf(0xFF.toByte())
        source.toUInt() `should be equal to` 255U
    }

    @Test
    fun `When converting max width ByteArray of 0xFF to Int, the result is Int MAX_VALUE`() {
        val source = ByteArray(UInt.SIZE_BYTES) { 0xFF.toByte() }
        source.toUInt() `should be equal to` UInt.MAX_VALUE
    }

    @Test
    fun `When converting empty ByteArray to Int, the result is 0`() {
        val source = byteArrayOf()
        source.toUInt() `should be equal to` 0U
    }

    @Test
    fun `When converting too wide ByteArray to Int, an exception is thrown`() {
        val source = ByteArray(Int.SIZE_BYTES + 1) { 0x00.toByte() }
        invoking { source.toUInt() } `should throw` IllegalArgumentException::class
    }

    @Test
    fun `When converting BigEndian ByteArray 0xF0000000 to Int, the result is 1`() {
        val source = ByteArray(UInt.SIZE_BYTES) { index -> if (index == 0) 0x01.toByte() else 0x00.toByte() }
        source.toUInt(bigEndian = true) `should be equal to` 1U
    }
}
