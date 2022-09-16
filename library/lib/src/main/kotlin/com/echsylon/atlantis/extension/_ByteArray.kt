package com.echsylon.atlantis.extension

fun ByteArray.toULong(bigEndian: Boolean = false): ULong {
    if (size > ULong.SIZE_BYTES)
        throw IllegalArgumentException("ByteArray to long to fit in ULong")

    val bytes = copyOf()
    if (bigEndian) bytes.reverse()

    var shift = size * Byte.SIZE_BITS
    var result: ULong = 0UL

    bytes.forEach {
        shift -= Byte.SIZE_BITS
        result = result or ((it.toULong() and 0xFFU) shl shift)
    }

    return result
}

fun ByteArray.toUInt(bigEndian: Boolean = false): UInt {
    if (size > UInt.SIZE_BYTES)
        throw IllegalArgumentException("ByteArray to long to fit in Int")

    val bytes = copyOf()
    if (bigEndian) bytes.reverse()

    var shift = size * Byte.SIZE_BITS
    var result = 0U

    bytes.forEach {
        shift -= Byte.SIZE_BITS
        result = result or ((it.toUInt() and 0xFFU) shl shift)
    }

    return result
}
