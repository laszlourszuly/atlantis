package com.echsylon.atlantis;

import com.echsylon.atlantis.filter.DefaultRequestFilter;
import com.echsylon.atlantis.internal.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class contains all request templates the {@link Atlantis} local web
 * server will ever serve. This is the "mocked Internet".
 */
@SuppressWarnings("WeakerAccess")
public class Configuration implements Serializable {

    protected String fallbackBaseUrl = null;
    protected List<Request> requests = null;
    protected Request.Filter requestFilter = null;

    // Intentionally hidden constructor
    protected Configuration() {
    }

    /**
     * Returns the fallback base url for this configuration. If given, this is
     * the suggested real world base URL to target (replacing "localhost:8080")
     * if no configuration was found for a request.
     *
     * @return The fallback base url or null.
     */
    public String fallbackBaseUrl() {
        return fallbackBaseUrl;
    }

    /**
     * Returns a flag telling whether this configuration can present an
     * alternative route to a supposedly "real" response if no configuration is
     * found for a request.
     *
     * @return Boolean true if the configuration has a fallback base url, false
     * otherwise.
     */
    public boolean hasAlternativeRoute() {
        return Utils.notEmpty(fallbackBaseUrl);
    }

    /**
     * Returns the filter that matches any requests in this configuration
     * against the HTTP parameters the client is trying to target.
     * <p>
     * NOTE! This method should only be used internally.
     *
     * @return The request filter. May be null.
     */
    public Request.Filter requestFilter() {
        return requestFilter;
    }

    /**
     * Tries to find a request configuration. The actual logic behind the
     * matching is delegated to the current {@link com.echsylon.atlantis.Request.Filter
     * Request#Filter} implementation. If no particular request filter has been
     * given a default implementation will be used instead.
     *
     * @param url     The url.
     * @param method  The request method.
     * @param headers The request headers.
     * @return The first request configuration that matches the given criteria
     * or null.
     */
    public Request findRequest(String url, String method, Map<String, String> headers) {
        return requestFilter == null ?
                new DefaultRequestFilter().getRequest(requests, url, method, headers) :
                requestFilter.getRequest(requests, url, method, headers);
    }

    /**
     * Adds a request to a collection of requests managed by this configuration
     * object. This method ensures that null pointers are not added.
     *
     * @param request The new request being eligible to serve mock responses
     *                when this method returns. Null pointers are ignored.
     */
    protected void addRequest(Request request) {
        if (request != null) {
            if (requests == null)
                requests = new ArrayList<>();

            if (!requests.contains(request))
                requests.add(request);
        }
    }

    /**
     * This class offers means of building a configuration object directly from
     * code (as opposed to configure one in a JSON asset).
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder extends Configuration {

        /**
         * Adds a request to the configuration being built. This method doesn't
         * add null pointers.
         *
         * @param request The request to add.
         * @return This buildable configuration object, allowing chaining of
         * method calls.
         */
        public Builder withRequest(Request request) {
            if (request != null) {
                if (requests == null)
                    requests = new ArrayList<>();

                requests.add(request);
            }

            return this;
        }

        /**
         * Sets the request filter logic to use when matching a request to
         * serve.
         *
         * @param requestFilter The request filter implementation. May be null.
         * @return This buildable configuration object, allowing chaining of
         * method calls.
         */
        public Builder withRequestFilter(Request.Filter requestFilter) {
            this.requestFilter = requestFilter;
            return this;
        }

        /**
         * Sets the fallback base url to hit when no mocked request was found.
         *
         * @param realBaseUrl The real-world base URL, including scheme.
         * @return This buildable configuration object, allowing chaining of
         * method calls.
         */
        public Builder withFallbackBaseUrl(String realBaseUrl) {
            this.fallbackBaseUrl = realBaseUrl;
            return this;
        }

        /**
         * Returns a sealed configuration object which can not be further built
         * on.
         *
         * @return The final configuration object.
         */
        public Configuration build() {
            return this;
        }

    }

}
