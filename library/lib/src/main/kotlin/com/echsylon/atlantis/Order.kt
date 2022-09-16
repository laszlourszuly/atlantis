package com.echsylon.atlantis

/**
 * Defines the order in which mock responses and messages are served for a
 * given request pattern.
 */
enum class Order {
    /**
     * Serve the responses or messages one-by-one, one after the other, one for
     * each call to the request or each served response.
     */
    SEQUENTIAL,

    /**
     * Serve a single, random responses or messages for each call to the
     * request or each served response.
     */
    RANDOM,

    /**
     * Serve all messages for a response. This order isn't applicable for
     * requests (as a request can not result in more than one response).
     */
    BATCH;

    companion object {
        /**
         * Safely returns an Order enum corresponding to the given name, or
         * defaults to SEQUENTIAL if indeterminate.
         */
        fun from(string: String): Order =
            runCatching { Order.valueOf(string) }
                .getOrDefault(SEQUENTIAL)
    }
}
