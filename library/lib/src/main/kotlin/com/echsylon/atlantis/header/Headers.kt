package com.echsylon.atlantis.header

class Headers : ArrayList<String>() {
    val expectSolidContent: Boolean
        get() = findLast { it.matches("^content-length\\s*:\\s*[0-9]\\d*\$".toRegex(RegexOption.IGNORE_CASE)) }
            ?.isNotBlank()
            ?: false

    val expectChunkedContent: Boolean
        get() = findLast { it.matches("^transfer-encoding\\s*:\\s*chunked\$".toRegex(RegexOption.IGNORE_CASE)) }
            ?.isNotBlank()
            ?: false
}