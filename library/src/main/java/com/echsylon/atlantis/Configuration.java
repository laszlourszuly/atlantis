package com.echsylon.atlantis;

import com.echsylon.atlantis.filter.DefaultRequestFilter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class contains all request mockRequests the {@link Atlantis} local web
 * server will ever serve. This is the "mocked Internet".
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Configuration implements Serializable {

    /**
     * This class offers means of building a configuration object directly from
     * code (as opposed to configure one in a JSON asset or file).
     */
    public static final class Builder {
        private final Configuration configuration;

        /**
         * Creates a new builder based on an uninitialized configuration
         * object.
         */
        public Builder() {
            configuration = new Configuration();
        }

        /**
         * Adds a mockRequest to the configuration being built. This method
         * doesn't add null pointers.
         *
         * @param mockRequest The mockRequest to add.
         * @return This builder object, allowing chaining of method calls.
         */
        public Builder addRequest(final MockRequest mockRequest) {
            if (!configuration.mockRequests.contains(mockRequest))
                configuration.mockRequests.add(mockRequest);

            return this;
        }

        /**
         * Sets the request filter logic to use when matching a request to
         * serve.
         *
         * @param filter The request filter implementation. May be null.
         * @return This builder object, allowing chaining of method calls.
         */
        public Builder setRequestFilter(final MockRequest.Filter filter) {
            configuration.requestFilter = filter;
            return this;
        }

        /**
         * Sets the fallback base url to hit when no mocked request was found.
         *
         * @param realBaseUrl The real-world base URL, including scheme.
         * @return This builder object, allowing chaining of method calls.
         */
        public Builder setFallbackBaseUrl(final String realBaseUrl) {
            configuration.fallbackBaseUrl = realBaseUrl;
            return this;
        }

        /**
         * Returns a sealed configuration object which can not be further built
         * on.
         *
         * @return The final configuration object.
         */
        public Configuration build() {
            return configuration;
        }
    }


    private String fallbackBaseUrl = null;
    private volatile List<MockRequest> mockRequests = null;
    private transient MockRequest.Filter requestFilter = null;


    Configuration() {
        mockRequests = new ArrayList<>();
    }

    /**
     * Returns the fallback base url for this configuration. If given, this is
     * the suggested real world base URL to target (replacing "localhost") if no
     * configuration was found for a request.
     *
     * @return The fallback base url or null.
     */
    public String fallbackBaseUrl() {
        return fallbackBaseUrl;
    }

    /**
     * Returns an unmodifiable list of the currently tracked request
     * mockRequests in this configuration.
     *
     * @return A list of known request mockRequests. The list is unmodifiable as
     * per definition in {@link Collections#unmodifiableList(List)}.
     */
    public List<MockRequest> requests() {
        return Collections.unmodifiableList(mockRequests);
    }

    /**
     * Returns the request filter of this configuration.
     *
     * @return The request filter or null.
     */
    public MockRequest.Filter requestFilter() {
        return requestFilter;
    }

    /**
     * Returns a suitable request template for the provided parameters.
     *
     * @return The request filter. May be null.
     */
    MockRequest request(final Meta meta) {
        MockRequest.Filter filter = requestFilter == null ?
                new DefaultRequestFilter() :
                requestFilter;

        return filter.findRequest(meta.method(), meta.url(), meta.headers(), mockRequests);
    }

    /**
     * Overrides forcefully the request filter of this configuration.
     *
     * @param requestFilter The new request filter.
     */
    void setRequestFilter(MockRequest.Filter requestFilter) {
        this.requestFilter = requestFilter;
    }

    /**
     * Adds a mockRequest to the list of available mockRequests that have a
     * mocked response to serve. This method ensures that null pointers are not
     * added.
     *
     * @param mockRequest The new mockRequest being eligible to serve mock
     *                    responses when this method returns. Null pointers are
     *                    ignored.
     */
    void addRequest(final MockRequest mockRequest) {
        if (!mockRequests.contains(mockRequest))
            mockRequests.add(mockRequest);
    }
}
