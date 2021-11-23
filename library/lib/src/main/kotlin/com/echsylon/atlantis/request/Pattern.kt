package com.echsylon.atlantis.request

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
    var responseOrder: Order = Order.SEQUENTIAL,
    val headers: MutableList<String> = mutableListOf()
) {

    /**
     * Checks whether an intercepted HTTP request matches this configuraton
     * pattern.
     *
     * @return True if the regular expressions match the corresping request
     *         attributes and the request contains all pattern headers, false
     *         otherwise.
     */
    // FIXME: Move this method to the Configuration implementation.
    fun match(request: Request): Boolean {
        return verb.toRegex().matches(request.verb) &&
                path.toRegex().matches(request.path) &&
                protocol.toRegex().matches(request.protocol) &&
                request.headers.containsAll(headers)
    }
}