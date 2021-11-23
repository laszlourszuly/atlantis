package com.echsylon.atlantis

import com.echsylon.atlantis.request.Order.RANDOM
import com.echsylon.atlantis.request.Pattern
import com.echsylon.atlantis.request.Request
import com.echsylon.atlantis.response.Response

/**
 * Describes the request patterns configuration, along with the corresponding
 * mock responses to serve.
 */
class Configuration {
    private val filters: HashMap<Pattern, MutableList<Response>> = hashMapOf()

    /**
     * Looks for mock response to serve for the given intercepted client
     * HTTP request.
     *
     * @param request The client request.
     * @return A matching mock response, or null if none found.
     */
    fun findResponse(request: Request): Response? {
        return filters.entries
            .firstOrNull { entry -> entry.key.match(request) }
            ?.let { entry ->
                if (entry.key.responseOrder == RANDOM) entry.value.random()
                else entry.value.removeFirstOrNull()?.also { entry.value.add(it) }
            }
    }

    /**
     * Adds a mock response for a corresponding request pattern.
     *
     * @param pattern  The pattern describing which requests to serve the mock
     *                 response for.
     * @param response The mock response to serve.
     */
    fun addResponse(pattern: Pattern, response: Response) {
        filters.computeIfAbsent(pattern) { mutableListOf() }
            .add(response)
    }

    /**
     * Merges the given configuration into this instance.
     *
     * @param configuration The configuration to merge.
     */
    fun addConfiguration(configuration: Configuration) {
        filters.putAll(configuration.filters)
    }

    /**
     * Removes all request patterns and corresponding mock responses from this
     * configuration.
     */
    fun clear() {
        filters.clear()
    }
}