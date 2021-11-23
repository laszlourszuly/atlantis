package com.echsylon.atlantis.extension

import java.io.Closeable

/**
 * Closes a closeable while consuming any exceptions. The exception stack trace
 * is printed to the logs but not re-thrown.
 */
fun Closeable.closeSilently() {
    runCatching { close() }
        .onFailure { println("Failed to close a resource, ignoring silently") }
}