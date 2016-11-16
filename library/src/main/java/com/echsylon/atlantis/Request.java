package com.echsylon.atlantis;

import com.echsylon.atlantis.filter.DefaultResponseFilter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.echsylon.atlantis.internal.Utils.notAnyEmpty;

/**
 * This class represents a request template as the {@link
 * com.echsylon.atlantis.Atlantis Atlantis} local web server expects it.
 * Atlantis will use this template when trying to identify which response to
 * serve to a user-provided request.
 */
@SuppressWarnings("WeakerAccess")
public class Request extends HttpEntity implements Serializable {

    /**
     * This class offers means of building a request configuration directly from
     * code (as opposed to configure one in a JSON asset or file).
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder {
        private final Request request;

        /**
         * Creates a new builder based on an uninitialized request object.
         */
        public Builder() {
            request = new Request();
        }

        /**
         * Creates a new builder based on the given request object.
         *
         * @param request The request object to build on. Must not be null.
         */
        public Builder(Request request) {
            this.request = request;
        }

        /**
         * Adds a header to the request being built. This method doesn't add
         * anything if either {@code key} or {@code value} is empty (null
         * pointers are considered empty).
         *
         * @param key   The header key.
         * @param value The header value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withHeader(String key, String value) {
            if (notAnyEmpty(key, value)) {
                if (request.headers == null)
                    request.headers = new HashMap<>();

                request.headers.put(key, value);
            }

            return this;
        }

        /**
         * Adds all non-empty key/value pairs from the given headers.
         *
         * @param headers The headers to copy keys and values from.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withHeaders(Map<String, String> headers) {
            if (headers != null) {
                if (request.headers == null)
                    request.headers = new HashMap<>();

                for (Map.Entry<String, String> entry : headers.entrySet())
                    if (notAnyEmpty(entry.getKey(), entry.getValue()))
                        request.headers.put(entry.getKey(), entry.getValue());
            }

            return this;
        }

        /**
         * Adds a response to the request being built. This method doesn't add
         * null pointers.
         *
         * @param response The response to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withResponse(Response response) {
            if (response != null) {
                if (request.responses == null)
                    request.responses = new ArrayList<>();

                request.responses.add(response);
            }

            return this;
        }

        /**
         * Sets the method of the request. Allows null pointers and empty
         * strings even though in practice such values doesn't make any sense.
         *
         * @param method The new HTTP request method.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withMethod(String method) {
            request.method = method;
            return this;
        }

        /**
         * Sets the url of the request. Allows null pointers and empty strings
         * even though in practice such values doesn't make any sense.
         *
         * @param url The new url.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withUrl(String url) {
            request.url = url;
            return this;
        }

        /**
         * Sets the response filter logic to use when deciding which response to
         * serve.
         *
         * @param responseFilter The response filter implementation. May be null.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withResponseFilter(Response.Filter responseFilter) {
            request.responseFilter = responseFilter;
            return this;
        }

        /**
         * Returns a sealed request object which can not be further built on.
         *
         * @return The final request object.
         */
        public Request build() {
            return request;
        }

    }

    protected String method = null;
    protected String url = null;
    protected List<Response> responses = null;
    protected transient Response.Filter responseFilter = null;

    // Intentionally hidden constructor.
    protected Request() {
    }

    /**
     * Returns the method of this request.
     *
     * @return The request method.
     */
    public String method() {
        return method;
    }

    /**
     * Returns the request url.
     *
     * @return The request url.
     */
    public String url() {
        return url;
    }

    /**
     * Returns the filter that picks a response to serve for this request.
     *
     * @return The response filter. Should never be null.
     */
    public Response.Filter responseFilter() {
        return responseFilter;
    }

    /**
     * Returns the response for this request, as filtered by the current
     * response filter.
     *
     * @return The mocked response.
     */
    public Response response() {
        return responseFilter == null ?
                new DefaultResponseFilter().getResponse(this, responses) :
                responseFilter.getResponse(this, responses);
    }

    /**
     * This interface describes the features for filtering out a particular
     * request template based on a set of hints describing the desired result.
     */
    public interface Filter {

        /**
         * Returns a request template based on the implemented filtering logic.
         *
         * @param requests All available request templates.
         * @param url      The url giving a hint of which template to find.
         * @param method   The corresponding request method to filter on.
         * @param headers  The headers hint.
         * @return The filtered request, or null if no match found.
         */
        Request getRequest(List<Request> requests, String url, String method, Map<String, String> headers);

    }

}
