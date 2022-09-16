package com.echsylon.atlantis.extension

import kotlin.text.RegexOption.IGNORE_CASE

/**
 * Checks whether there is data within the list that indicates a
 * deterministic size of the corresponding content.
 */
val ArrayList<String>.expectSolidContent: Boolean
    get() = findLast { it.matches("^content-length\\s*:\\s*\\d+\$".toRegex(IGNORE_CASE)) }
        ?.isNotBlank()
        ?: false

/**
 * Checks whether there is data within the list that indicates a
 * chunked transfer of the corresponding content.
 */
val ArrayList<String>.expectChunkedContent: Boolean
    get() = findLast { it.matches("^transfer-encoding\\s*:\\s*chunked\$".toRegex(IGNORE_CASE)) }
        ?.isNotBlank()
        ?: false

/**
 * Returns the value of the last web socket key header, or null
 * if no such header exists.
 */
val ArrayList<String>.webSocketKey: String?
    get() = findLast { it.matches("^sec-websocket-key\\s*:\\s*[a-zA-Z0-9+/=]*\$".toRegex(IGNORE_CASE)) }
        ?.split(":".toRegex(), 2)
        ?.takeIf { it.size == 2 }
        ?.last()
        ?.trim()

/**
 * The value of the web socket accept header.
 */
var ArrayList<String>.webSocketAccept: String?
    get() = findLast { it.matches("^sec-websocket-accept\\s*:\\s*[a-zA-Z0-9+/=]*\$".toRegex(IGNORE_CASE)) }
        ?.split(":".toRegex(), 2)
        ?.takeIf { it.size == 2 }
        ?.last()
        ?.trim()
    set(value) = removeAll { it.matches("^sec-websocket-accept\\s*:.*\$".toRegex(IGNORE_CASE)) }
        .also { add("Sec-WebSocket-Accept: $value") }
        .let { }

/**
 * The value of the upgrade header.
 */
var ArrayList<String>.upgrade: String?
    get() = findLast { it.matches("^upgrade\\s*:\\s*.*\$".toRegex(IGNORE_CASE)) }
        ?.split(":".toRegex(), 2)
        ?.takeIf { it.size == 2 }
        ?.last()
        ?.trim()
    set(value) = removeAll { it.matches("^upgrade\\s*:\\s*.*\$".toRegex(IGNORE_CASE)) }
        .also { add("Upgrade: $value") }
        .let { }

/**
 * The value of the connection header.
 */
var ArrayList<String>.connection: String?
    get() = findLast { it.matches("^connection\\s*:\\s*.*\$".toRegex(IGNORE_CASE)) }
        ?.split(":".toRegex(), 2)
        ?.takeIf { it.size == 2 }
        ?.last()
        ?.trim()
    set(value) = removeAll { it.matches("^connection\\s*:\\s*.*\$".toRegex(IGNORE_CASE)) }
        .also { add("Connection: $value") }
        .let { }

/**
 * The value of the content length header.
 */
var ArrayList<String>.contentLength: Int?
    get() = findLast { it.matches("^content-length\\s*:\\s*\\d+\$".toRegex(IGNORE_CASE)) }
        ?.split(":".toRegex(), 2)
        ?.takeIf { it.size == 2 }
        ?.last()
        ?.trim()
        ?.toInt()
    set(value) = removeAll { it.matches("^content-length\\s*:.*\$".toRegex(IGNORE_CASE)) }
        .also { add("Content-Length: $value") }
        .let { }
