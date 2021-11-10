package com.echsylon.atlantis.extension

fun ULongRange.isZero(): Boolean {
    return first == 0UL && last == 0UL
}
