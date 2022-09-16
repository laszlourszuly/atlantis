package com.echsylon.atlantis.extension

fun String.encodeHexStringAsByteArray(): ByteArray {
    check(length % 2 == 0) { "Length must be even" }
    check(Regex("^(0x|0X)?[0-9a-fA-F]+$").matches(this)) { "Not a hex string" }
    return replace("^(0x|0X)".toRegex(), "")
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
