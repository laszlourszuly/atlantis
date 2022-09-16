package com.echsylon.atlantis.extension

import kotlin.math.max
import kotlin.math.min

/**
 * Checks if both the lower bound and upper bound of this range has the value
 * zero (0).
 */
fun IntRange.isZero(): Boolean {
    val string: String? = null
    string.isNullOrEmpty()
    return first == 0 && last == 0
}

fun IntRange.trim(cap: IntRange): IntRange {
    return when {
        isZero() -> IntRange(cap.first, cap.last)
        isEmpty() -> IntRange(cap.first, cap.last)
        else -> IntRange(max(first, cap.first), min(last, cap.last))
    }
}

fun IntRange.toRandomDelay(): Int {
    val start = max(0, first)
    val stop = max(start, last)
    return (start..stop).random()
}

fun IntRange.toRandomSize(): Int {
    val min = max(0, first)
    val max = max(min, last)
    return if (max == 0) Int.MAX_VALUE else (min..max).random()
}


