package com.echsylon.atlantis.template;

import com.echsylon.atlantis.ResponseFilter;
import com.echsylon.atlantis.filter.response.DefaultResponseFilter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.echsylon.atlantis.internal.Utils.notAnyEmpty;

/**
 * This class represents a request template as the {@link com.echsylon.atlantis.Atlantis Atlantis}
 * local web server expects it. It will use this template when trying to identify which response to
 * serve to a user provided request.
 */
public class Request extends HttpEntity implements Serializable {

    /**
     * This class offers means of building a request configuration directly from code (as opposed to
     * configure one in a JSON asset).
     */
    public static final class Builder extends Request {

        /**
         * Adds a header to the request being built. Doesn't add anything if either {@param key} or
         * {@param value} is empty (null pointer is considered as empty).
         *
         * @param key   The header key.
         * @param value The header value.
         * @return This buildable request instance, allowing chaining of method calls.
         */
        public Builder withHeader(String key, String value) {
            if (notAnyEmpty(key, value)) {
                if (this.headers == null)
                    this.headers = new HashMap<>();

                this.headers.put(key, value);
            }

            return this;
        }

        /**
         * Adds all non-empty key/value pairs from the given headers.
         *
         * @param headers The headers to copy keys and values from.
         * @return This buildable request instance, allowing chaining of method calls.
         */
        public Builder withHeaders(Map<String, String> headers) {
            if (headers != null) {
                if (this.headers == null)
                    this.headers = new HashMap<>();

                for (Map.Entry<String, String> entry : headers.entrySet())
                    if (notAnyEmpty(entry.getKey(), entry.getValue()))
                        this.headers.put(entry.getKey(), entry.getValue());
            }

            return this;
        }

        /**
         * Adds a response to the request being built. Doesn't add null pointers.
         *
         * @param response The response to add.
         * @return This buildable request instance, allowing chaining of method calls.
         */
        public Builder withResponse(Response response) {
            if (response != null) {
                if (this.responses == null)
                    this.responses = new ArrayList<>();

                this.responses.add(response);
            }

            return this;
        }

        /**
         * Sets the method of the request.  Allows null pointers and empty strings, even though it
         * in practice doesn't make any sense.
         *
         * @param method The new HTTP request method.
         * @return This buildable request instance, allowing chaining of method calls.
         */
        public Builder withMethod(String method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the url of the request. Allows null pointers and empty strings, even though it in
         * practice doesn't make any sense.
         *
         * @param url The new url.
         * @return This buildable request instance, allowing chaining of method calls.
         */
        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the response filter logic to use when deciding which response to serve. Null is a
         * valid value, even though it doesn't make sense.
         *
         * @param responseFilter The response filter implementation.
         * @return This buildable request instance, allowing chaining of method calls.
         */
        public Builder withResponseFilter(ResponseFilter responseFilter) {
            this.responseFilter = responseFilter;
            return this;
        }

    }

    protected String method = null;
    protected String url = null;
    protected List<Response> responses = null;
    protected ResponseFilter responseFilter = new DefaultResponseFilter();

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
     * Returns the corresponding response for this request.
     *
     * @return The response.
     */
    public Response response() {
        return responseFilter != null ?
                responseFilter.getResponse(this, responses) :
                null;
    }

}
