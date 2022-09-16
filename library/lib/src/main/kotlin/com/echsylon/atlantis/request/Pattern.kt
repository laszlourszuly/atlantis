package com.echsylon.atlantis.request

import com.echsylon.atlantis.Order
import com.echsylon.atlantis.Order.SEQUENTIAL

/**
 * Defines the configurable properties of a request pattern in the realm of
 * Atlantis. Each intercepted client HTTP request is matched against the
 * configured patterns in order to determine which mock response to serve.
 *
 * @param verb          The request verb regex.
 * @param path          The request path regex.
 * @param protocol      The HTTP protocol regex.
 * @param responseOrder The order any mocked responses are served in.
 * @param headers       The minimum subset of request headers for a match.
 */
data class Pattern(
    var verb: String = "GET",
    var path: String = "/.*",
    var protocol: String = "HTTP/1.1",
    var responseOrder: Order = SEQUENTIAL,
    val headers: MutableList<String> = mutableListOf()
)
