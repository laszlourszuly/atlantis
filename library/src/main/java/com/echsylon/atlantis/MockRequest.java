package com.echsylon.atlantis;

import com.echsylon.atlantis.filter.DefaultResponseFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.echsylon.atlantis.Utils.getNonNull;

/**
 * This class contains the full description of a request that should get a mock
 * response.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MockRequest {

    /**
     * This interface describes the mandatory feature set to provide a request
     * template. Implementing classes are responsible for picking a request
     * template from a given list of available templates.
     */
    public interface Filter {

        /**
         * Returns a request template of choice, based on the given parameters.
         * May be null, in which case the calling logic is responsible for
         * deciding which request template to use.
         *
         * @param method   The request method ("GET", "POST", etc).
         * @param url      The request url (e.g. "/path/to/resource").
         * @param headers  The request headers map. Multiple header values with
         *                 the same key are merged, separated by a comma (',').
         * @param requests All request templates available.
         * @return The request template candidate.
         */
        MockRequest findRequest(final String method,
                                final String url,
                                final Map<String, String> headers,
                                final List<MockRequest> requests);
    }


    /**
     * This class offers means of building a request template directly from code
     * (as opposed to configure one in a JSON file).
     */
    public static final class Builder {
        private final MockRequest mockRequest;

        /**
         * Creates a new builder based on an uninitialized request template
         * object.
         */
        public Builder() {
            this(new MockRequest());
        }

        /**
         * Creates a new builder based on the provided mock request object.
         *
         * @param source The mock request to initialize this builder with.
         */
        public Builder(final MockRequest source) {
            mockRequest = source != null ? source : new MockRequest();
        }

        /**
         * Offers (internal) help to easily create a mock request from a meta.
         *
         * @param meta The request meta data. Null is handled gracefully.
         */
        Builder(final Meta meta) {
            mockRequest = new MockRequest();
            if (meta != null) {
                mockRequest.url = meta.url();
                mockRequest.method = meta.method();
                mockRequest.headerManager = meta.headerManager();
            }
        }

        /**
         * Sets the header manager of the mock response being built. This method
         * is meant for internal use only.
         *
         * @param headerManager The new header manager. Null is handled
         *                      gracefully.
         * @return This builder instance, allowing chaining of method calls.
         */
        Builder setHeaderManager(final HeaderManager headerManager) {
            mockRequest.headerManager = headerManager == null ?
                    new HeaderManager() :
                    headerManager;
            return this;
        }

        /**
         * Adds a header to the mock request being built, replacing any and all
         * existing values for the same key.
         *
         * @param key   The header key.
         * @param value The new header value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setHeader(final String key, final String value) {
            mockRequest.headerManager.set(key, value);
            return this;
        }

        /**
         * Adds a header to the request template being built. Doesn't add
         * anything if the {@code key} or {@code value} is empty or null.
         *
         * @param key   The header key.
         * @param value The header value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeader(final String key, final String value) {
            mockRequest.headerManager.add(key, value);
            return this;
        }

        /**
         * Adds all non-empty header keys and values to the mock request being
         * built.
         *
         * @param key    The header key.
         * @param values The header values.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeaders(final String key, final List<String> values) {
            mockRequest.headerManager.add(key, values);
            return this;
        }

        /**
         * Adds all non-empty header keys and values to the mock request being
         * built.
         *
         * @param key    The header key.
         * @param values The header values.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeaders(final String key, final String... values) {
            mockRequest.headerManager.add(key, Arrays.asList(values));
            return this;
        }

        /**
         * Adds all headers to the request template being built where neither
         * the key nor the value is empty or null.
         *
         * @param headers The headers to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeaders(final Map<String, List<String>> headers) {
            mockRequest.headerManager.add(headers);
            return this;
        }

        /**
         * Adds a mock mockResponse to the request template being built.
         *
         * @param mockResponse The mockResponse to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addResponse(final MockResponse mockResponse) {
            if (!mockRequest.responses.contains(mockResponse))
                mockRequest.responses.add(mockResponse);
            return this;
        }

        /**
         * Sets the method of the request template being built.
         *
         * @param method The new HTTP request method.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setMethod(final String method) {
            mockRequest.method = method;
            return this;
        }

        /**
         * Sets the url of the request template being built.
         *
         * @param url The new url.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setUrl(final String url) {
            mockRequest.url = url;
            return this;
        }

        /**
         * Sets the response filter logic of the request template being built.
         * This filter can be used when deciding which mock response to serve.
         *
         * @param responseFilter The response filter implementation.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setResponseFilter(final MockResponse.Filter responseFilter) {
            mockRequest.responseFilter = responseFilter;
            return this;
        }

        /**
         * Returns a sealed request template object which can not be further
         * built on.
         *
         * @return The final request template object.
         */
        public MockRequest build() {
            return mockRequest;
        }
    }


    private String url = null;
    private String method = null;
    private List<MockResponse> responses = null;
    private transient HeaderManager headerManager = null;
    private transient MockResponse.Filter responseFilter = null;


    MockRequest() {
        headerManager = new HeaderManager();
        responses = new ArrayList<>();
    }

    /**
     * Returns the method of this request template.
     *
     * @return The request method.
     */
    public String method() {
        return getNonNull(method, "");
    }

    /**
     * Returns the request url.
     *
     * @return The request url.
     */
    public String url() {
        return getNonNull(url, "");
    }

    /**
     * Returns an unmodifiable list of the currently configured mock responses
     * for this request template.
     *
     * @return A list of mock responses. The list is unmodifiable as per
     * definition in {@link Collections#unmodifiableList(List)}.
     */
    public List<MockResponse> responses() {
        return Collections.unmodifiableList(responses);
    }

    /**
     * Returns the response filter of this request template.
     *
     * @return The response filter or null.
     */
    public MockResponse.Filter responseFilter() {
        return responseFilter;
    }

    /**
     * Returns the header manager.
     *
     * @return The response header manager. Never null.
     */
    public HeaderManager headerManager() {
        return headerManager;
    }

    /**
     * Returns a suitable mocked response to serve for this request template.
     *
     * @return The response to server, may be null.
     */
    MockResponse response() {
        return responseFilter == null ?
                new DefaultResponseFilter().findResponse(responses) :
                responseFilter.findResponse(responses);
    }

    /**
     * Forcefully overrides the response filter of this request template.
     *
     * @param responseFilter The new response filter.
     */
    void setResponseFilter(final MockResponse.Filter responseFilter) {
        this.responseFilter = responseFilter;
    }
}
