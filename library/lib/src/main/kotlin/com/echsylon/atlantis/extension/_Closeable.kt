package com.echsylon.atlantis.extension

import java.io.Closeable

fun Closeable.closeSilently() {
    runCatching { close() }
        .onFailure { println("Failed to close a resource, ignoring silently") }
}