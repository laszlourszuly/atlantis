package com.echsylon.atlantis.message

/**
 * Frame types handled by this implementation.
 */
enum class Type {
    TEXT, DATA, PING, PONG, CLOSE;

    companion object {
        fun fromString(string: String): Type? =
            Type.values().associateBy { it.name.lowercase() }[string.lowercase()]
    }
}
