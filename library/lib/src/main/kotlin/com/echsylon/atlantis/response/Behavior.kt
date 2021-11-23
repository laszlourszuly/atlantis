package com.echsylon.atlantis.response

/**
 * Describes the mock response serve behavior.
 *
 * @param delay           The range of random milliseconds to delay any given
 *                        chunk of the mock response body.
 * @param chunk           The range of random bytes to "chunk" up the mock
 *                        response body in. Note that this has nothing to do
 *                        with the HTTP "Transfer-Encoding: chunked" header.
 * @param calculateLength Whether to calculate the mocked response body length
 *                        and set the corresponding Content-Length header. It's
 *                        up to the underlaying mock web server to decide if
 *                        any existing mock headers are to be replaced or not.
 */
data class Behavior(
    var delay: ULongRange = 0UL..0UL,
    var chunk: ULongRange = 0UL..0UL,
    var calculateLength: Boolean = false
)