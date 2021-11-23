package com.echsylon.atlantis.header

/**
 * Describes a list of HTTP headers. As such it offers additional helper
 * attributes to determine frequently requested properties of the data within. 
 */
class Headers : ArrayList<String>() {

    /**
     * Checks whether there is data within the list that indicates a
     * deterministic size of the corresponding content.
     */
    val expectSolidContent: Boolean
        get() = findLast { it.matches("^content-length\\s*:\\s*[0-9]\\d*\$".toRegex(RegexOption.IGNORE_CASE)) }
            ?.isNotBlank()
            ?: false

    /**
     * Checks whether there is data within the list that indicates a
     * chunked transfer of the corresponding content. 
     */
    val expectChunkedContent: Boolean
        get() = findLast { it.matches("^transfer-encoding\\s*:\\s*chunked\$".toRegex(RegexOption.IGNORE_CASE)) }
            ?.isNotBlank()
            ?: false
}