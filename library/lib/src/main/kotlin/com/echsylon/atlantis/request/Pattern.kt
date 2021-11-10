package com.echsylon.atlantis.request

data class Pattern(
    var verb: String = "GET",
    var path: String = "/.*",
    var protocol: String = "HTTP/1.1",
    var responseOrder: Order = Order.SEQUENTIAL,
    val headers: MutableList<String> = mutableListOf()
) {
    fun match(request: Request): Boolean {
        return verb.toRegex().matches(request.verb) &&
                path.toRegex().matches(request.path) &&
                protocol.toRegex().matches(request.protocol) &&
                request.headers.containsAll(headers)
    }
}