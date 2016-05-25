package com.echsylon.atlantis.template;

import com.echsylon.atlantis.filter.request.DefaultRequestFilter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class contains all request templates the {@link com.echsylon.atlantis.Atlantis Atlantis}
 * local web server will ever serve. This is the "downloaded Internet".
 */
public class Configuration implements Serializable {

    /**
     * This class offers means of building a configuration object directly from code (as opposed to
     * configure one in a JSON asset).
     */
    public static final class Builder extends Configuration {

        /**
         * Adds a request to the configuration being built. Doesn't add null pointers.
         *
         * @param request The request to add.
         * @return This buildable configuration object, allowing chaining of method calls.
         */
        public Builder withRequest(Request request) {
            if (request != null) {
                if (this.requests == null)
                    this.requests = new ArrayList<>();

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

    }

    protected List<Request> requests = null;
    protected Request.Filter requestFilter = new DefaultRequestFilter();

    // Intentionally hidden constructor
    protected Configuration() {
    }

    /**
     * Tries to find a request template that matches the given method, url and header criteria.
     * Returns the first match.
     *
     * @param url     The url.
     * @param method  The request method.
     * @param headers The request headers.
     * @return The request template that matches the given criteria or null if no match found.
     */
    public Request findRequest(String url, String method, Map<String, String> headers) {
        return requestFilter != null ?
                requestFilter.getRequest(requests, url, method, headers) :
                null;
    }

}
