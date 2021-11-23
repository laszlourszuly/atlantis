package com.echsylon.atlantis.extension

/**
 * Checks if both the lower bound and upper bound of this range has the value
 * zero (0L).
 */
fun ULongRange.isZero(): Boolean {
    return first == 0UL && last == 0UL
}
