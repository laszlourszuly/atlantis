package com.echsylon.atlantis.extension

import org.amshove.kluent.AnyException
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.junit.Test

class StringExtensionTest {
    @Test
    fun `When encoding even length hex string with upper case letters, it is correctly translated to byte array`() {
        "0x7E".encodeHexStringAsByteArray() `should be equal to` byteArrayOf(0x7E)
    }

    @Test
    fun `When encoding even length hex string with lower case letters, it is correctly translated to byte array`() {
        "0x1a".encodeHexStringAsByteArray() `should be equal to` byteArrayOf(0x1a)
    }

    @Test
    fun `When encoding even length hex string with leading 0x, it is correctly translated to byte array`() {
        "0x7E".encodeHexStringAsByteArray() `should be equal to` byteArrayOf(0x7E)
    }

    @Test
    fun `When encoding even length hex string with leading 0X, it is correctly translated to byte array`() {
        "0X01".encodeHexStringAsByteArray() `should be equal to` byteArrayOf(0x01)
    }

    @Test
    fun `When encoding even length hex string without leading 0x or 0X, it is correctly translated to byte array`(){
        "7F0a".encodeHexStringAsByteArray() `should be equal to` byteArrayOf(0x7F, 0x0A)
    }

    @Test
    fun `When encoding odd length hex string, an exception is thrown`() {
        invoking { "0x123".encodeHexStringAsByteArray() } `should throw` AnyException
    }

    @Test
    fun `When encoding even length hex string with invalid alphanumeric characters, an exception is thrown`() {
        invoking { "0x7Z".encodeHexStringAsByteArray() } `should throw` AnyException
    }

    @Test
    fun `When encoding even length hex string with non-alphanumeric characters, an exception is thrown`() {
        invoking { "0x0%".encodeHexStringAsByteArray() } `should throw` AnyException
    }
}
