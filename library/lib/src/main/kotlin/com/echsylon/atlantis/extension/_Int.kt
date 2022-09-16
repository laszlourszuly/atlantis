package com.echsylon.atlantis.extension

/**
 * Adds a convenience formatting function to all Int values.
 */
fun Int.toHexString(): String = java.lang.Long.toHexString(this.toLong())

fun Int.toRange(): IntRange = this..this
