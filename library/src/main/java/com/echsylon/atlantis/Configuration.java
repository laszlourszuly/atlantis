package com.echsylon.atlantis;

import com.echsylon.atlantis.filter.DefaultRequestFilter;
import com.echsylon.atlantis.internal.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class contains all request templates the {@link com.echsylon.atlantis.Atlantis Atlantis}
 * local web server will ever serve. This is the "mocked Internet".
 */
@SuppressWarnings("WeakerAccess")
public class Configuration implements Serializable {

    /**
     * This class offers means of building a configuration object directly from code (as opposed to
     * configure one in a JSON asset).
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder extends Configuration {

        /**
         * Adds a request to the configuration being built. Doesn't add null pointers.
         *
         * @param request The request to add.
         * @return This buildable configuration object, allowing chaining of method calls.
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
         * Sets the request filter logic to use when matching a request to serve. Null is a valid
         * value, even though it doesn't make sense.
         *
         * @param requestFilter The request filter implementation.
         * @return This buildable configuration object, allowing chaining of method calls.
         */
        public Builder withRequestFilter(Request.Filter requestFilter) {
            this.requestFilter = requestFilter;
            return this;
        }

        /**
         * Sets the fallback base url to try to hit when no no mocked request was found.
         *
         * @param realBaseUrl The real-world base URL, including scheme.
         * @return This buildable configuration object, allowing chaining of method calls.
         */
        public Builder withFallbackBaseUrl(String realBaseUrl) {
            this.fallbackBaseUrl = realBaseUrl;
            return this;
        }

        /**
         * Returns a sealed configuration object which can not be further built on.
         *
         * @return The final configuration object.
         */
        public Configuration build() {
            return this;
        }

    }

    protected String fallbackBaseUrl = null;
    protected List<Request> requests = null;
    protected Request.Filter requestFilter = new DefaultRequestFilter();

    // Intentionally hidden constructor
    protected Configuration() {
    }

    /**
     * Returns the fallback base url for this configuration.
     *
     * @return The fallback base url or null.
     */
    public String fallbackBaseUrl() {
        return fallbackBaseUrl;
    }

    /**
     * Returns a flag telling whether this configuration can present an alternative route to a
     * supposedly "real" response.
     *
     * @return Boolean true if the configuration has a fallback base url, false otherwise.
     */
    public boolean hasAlternativeRoute() {
        return Utils.notEmpty(fallbackBaseUrl);
    }

    /**
     * Tries to find a request configuration. The actual logic behind the matching is delegated to
     * the current {@link com.echsylon.atlantis.Request.Filter} implementation.
     *
     * @param url     The url.
     * @param method  The request method.
     * @param headers The request headers.
     * @return The first request configuration that matches the given criteria or null.
     */
    public Request findRequest(String url, String method, Map<String, String> headers) {
        return requestFilter != null ?
                requestFilter.getRequest(requests, url, method, headers) :
                null;
    }

}
