package com.echsylon.atlantis;

import com.echsylon.atlantis.filter.DefaultResponseFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.echsylon.atlantis.Utils.getNonNull;

/**
 * This class contains the full description of a response mockRequest.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MockRequest {

    /**
     * This interface describes the mandatory feature set to provide a
     * mockRequest mockRequest. Implementing classes are responsible for picking
     * a mockRequest from a given list of available templates.
     */
    public interface Filter {

        /**
         * Returns a mockRequest mockRequest of choice, based on the given
         * parameters. May be null, in which case the calling logic is
         * responsible for deciding which mockRequest to use.
         *
         * @param method       The mockRequest method ("GET", "POST", etc) from
         *                     the HTTP client.
         * @param url          The mockRequest url (e.g. "/path/to/resource")
         *                     from the HTTP client.
         * @param headers      The header key/value pairs from the HTTP client.
         * @param mockRequests The complete list of all available mockRequest
         *                     mockRequests to pick from.
         * @return The mockRequest mockRequest candidate.
         */
        MockRequest findRequest(final String method,
                                final String url,
                                final Map<String, String> headers,
                                final List<MockRequest> mockRequests);
    }


    /**
     * This class offers means of building a mockRequest mockRequest directly
     * from code (as opposed to configure one in a JSON file).
     */
    public static final class Builder {
        private final MockRequest mockRequest;

        /**
         * Creates a new builder based on an uninitialized mockRequest object.
         */
        public Builder() {
            mockRequest = new MockRequest();
        }

        /**
         * Adds a header to the mockRequest being built. Doesn't add anything if
         * the {@code key} or {@code value} is empty or null.
         *
         * @param key   The header key.
         * @param value The header value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeader(final String key, final String value) {
            mockRequest.headers.put(key, value);
            return this;
        }

        /**
         * Adds all headers to the mockRequest mockRequest being built where
         * neither the key nor the value is empty or null.
         *
         * @param headers The headers to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeaders(final Map<String, String> headers) {
            mockRequest.headers.putAll(headers);
            return this;
        }

        /**
         * Adds a mock mockResponse to the mockRequest mockRequest being built.
         *
         * @param mockResponse The mockResponse to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addResponse(final MockResponse mockResponse) {
            if (!mockRequest.mockResponses.contains(mockResponse))
                mockRequest.mockResponses.add(mockResponse);
            return this;
        }

        /**
         * Sets the method of the mockRequest mockRequest being built.
         *
         * @param method The new HTTP mockRequest method.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setMethod(final String method) {
            mockRequest.method = method;
            return this;
        }

        /**
         * Sets the url of the mockRequest being built.
         *
         * @param url The new url.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setUrl(final String url) {
            mockRequest.url = url;
            return this;
        }

        /**
         * Sets the response filter logic of the mockRequest being built. This
         * filter can be used when deciding which mock response to serve.
         *
         * @param responseFilter The response filter implementation.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setResponseFilter(final MockResponse.Filter responseFilter) {
            mockRequest.responseFilter = responseFilter;
            return this;
        }

        /**
         * Returns a sealed mockRequest object which can not be further built
         * on.
         *
         * @return The final mockRequest object.
         */
        public MockRequest build() {
            return mockRequest;
        }
    }


    private String url = null;
    private String method = null;
    private List<MockResponse> mockResponses = null;
    private HeaderManager headers = null;
    private transient MockResponse.Filter responseFilter = null;


    MockRequest() {
        headers = new HeaderManager();
        mockResponses = new ArrayList<>();
    }

    /**
     * Returns the method of this mockRequest.
     *
     * @return The mockRequest method.
     */
    public String method() {
        return getNonNull(method, "");
    }

    /**
     * Returns the mockRequest url.
     *
     * @return The mockRequest url.
     */
    public String url() {
        return getNonNull(url, "");
    }

    /**
     * Returns an unmodifiable map of required header key/value pairs for this
     * mockRequest mockRequest.
     *
     * @return The unmodifiable mockRequest headers map as per definition in
     * {@link Collections#unmodifiableMap(Map)}.
     */
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Returns an unmodifiable list of the currently configured mock
     * mockResponses for this mockRequest mockRequest.
     *
     * @return A list of mock mockResponses. The list is unmodifiable as per
     * definition in {@link Collections#unmodifiableList(List)}.
     */
    public List<MockResponse> responses() {
        return Collections.unmodifiableList(mockResponses);
    }

    /**
     * Returns the response filter of this mockRequest mockRequest.
     *
     * @return The response filter or null.
     */
    public MockResponse.Filter responseFilter() {
        return responseFilter;
    }

    /**
     * Returns a suitable mocked response to serve for this mockRequest.
     *
     * @return The response to server, may be null.
     */
    MockResponse response() {
        return responseFilter == null ?
                new DefaultResponseFilter().findResponse(mockResponses) :
                responseFilter.findResponse(mockResponses);
    }

    /**
     * Overrides forcefully the response filter of this mockRequest
     * mockRequest.
     *
     * @param responseFilter The new response filter.
     */
    void setResponseFilter(final MockResponse.Filter responseFilter) {
        this.responseFilter = responseFilter;
    }

    /**
     * Returns a boolean flag indicating whether there is a "Content-Length"
     * header with a value greater than 0.
     *
     * @return Boolean true if there is a "Content-Length" header and a
     * corresponding value greater than 0, false otherwise.
     */
    boolean isExpectedToHaveBody() {
        return headers.isExpectedToHaveBody();
    }

    /**
     * Returns a boolean flag indicating whether there is a "Transfer-Encoding"
     * header with a "chunked" value.
     *
     * @return Boolean true if there is a "Transfer-Encoding" header and a
     * corresponding "chunked" value, false otherwise.
     */
    boolean isExpectedToBeChunked() {
        return headers.isExpectedToBeChunked();
    }
}
