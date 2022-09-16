package com.echsylon.atlantis

import com.echsylon.atlantis.Order.*
import com.echsylon.atlantis.request.Pattern
import com.echsylon.atlantis.request.Request
import com.echsylon.atlantis.response.Message
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
            .firstOrNull { (pattern, _) -> match(pattern, request) }
            ?.let { (pattern, responses) ->
                when (pattern.responseOrder) {
                    SEQUENTIAL -> responses.removeFirstOrNull()?.also { responses.add(it) }
                    RANDOM -> responses.randomOrNull()
                    else -> null
                }
            }
    }

    /**
     * Selects WebSocket messages from the response based on the message order
     * configured.
     *
     * @param response The response to pick a message from.
     * @return The list of selected messages.
     */
    fun findMessages(response: Response): List<Message> {
        return when (response.messageOrder) {
            SEQUENTIAL -> response.messages.removeFirstOrNull()
                ?.also { response.messages.add(it) }
                ?.let { listOf(it) }
                ?: emptyList()
            RANDOM -> response.messages.randomOrNull()
                ?.let { listOf(it) }
                ?: emptyList()
            BATCH -> response.messages
                .toMutableList() // return a copy
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

    /**
     * Checks whether an intercepted HTTP request matches the given pattern.
     *
     * @return True if the regular expressions match the corresponding request
     *         attributes and the request contains all pattern headers, false
     *         otherwise.
     */
    private fun match(pattern: Pattern, request: Request): Boolean {
        return pattern.verb.toRegex().matches(request.verb) &&
                pattern.path.toRegex().matches(request.path) &&
                pattern.protocol.toRegex().matches(request.protocol) &&
                request.headers.containsAll(pattern.headers)
    }

}
