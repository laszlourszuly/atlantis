package com.echsylon.atlantis.extension

/**
 * Adds a convenience formatting function to all Long values.
 */
fun Long.toHexString(): String = java.lang.Long.toHexString(this)
