package com.echsylon.atlantis.response

data class Behavior(
    var delay: ULongRange = 0UL..0UL,
    var chunk: ULongRange = 0UL..0UL,
    var calculateLength: Boolean = false
)