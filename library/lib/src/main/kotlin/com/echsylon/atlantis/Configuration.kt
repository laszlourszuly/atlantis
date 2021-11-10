package com.echsylon.atlantis

import com.echsylon.atlantis.request.Order.RANDOM
import com.echsylon.atlantis.request.Pattern
import com.echsylon.atlantis.request.Request
import com.echsylon.atlantis.response.Response

class Configuration {
    private val filters: HashMap<Pattern, MutableList<Response>> = hashMapOf()

    fun findResponse(request: Request): Response? {
        return filters.entries
            .firstOrNull { entry -> entry.key.match(request) }
            ?.let { entry ->
                if (entry.key.responseOrder == RANDOM) entry.value.random()
                else entry.value.removeFirstOrNull()?.also { entry.value.add(it) }
            }
    }

    fun addResponse(pattern: Pattern, response: Response) {
        filters.computeIfAbsent(pattern) { mutableListOf() }
            .add(response)
    }

    fun addConfiguration(configuration: Configuration) {
        filters.putAll(configuration.filters)
    }

    fun clear() {
        filters.clear()
    }
}