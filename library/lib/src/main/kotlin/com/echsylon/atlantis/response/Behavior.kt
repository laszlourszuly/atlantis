package com.echsylon.atlantis.response

/**
 * Describes the mock response serve behavior.
 *
 * @param delay             The range of random milliseconds to delay any given
 *                          chunk of the mock response body.
 * @param chunk             The range of random bytes to "chunk" up the mock
 *                          response body in. Note that this has nothing to do
 *                          with the HTTP "Transfer-Encoding: chunked" header but
 *                          is rather related to the "delay" behaviour.
 * @param calculateLength   Whether to calculate the mocked response body length
 *                          and set the corresponding Content-Length header.
 * @param calculateWsAccept Whether to calculate the response Sec-WebSocket-Accept
 *                          header based on the request headers.
 */
data class Behavior(
    var delay: IntRange = 0..0,
    var chunk: IntRange = 0..0,
    var calculateLength: Boolean = true,
    var calculateWsAccept: Boolean = true
)
