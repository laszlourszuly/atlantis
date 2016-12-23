package com.echsylon.atlantis;

import com.echsylon.atlantis.filter.DefaultRequestFilter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
            if (!configuration.requests.contains(mockRequest))
                configuration.requests.add(mockRequest);

            return this;
        }

        /**
         * Adds a default response header to the configuration being built. Any
         * existing keys will be overwritten. If a corresponding header exists
         * in a mock response definition, then that header will be honored
         * instead.
         *
         * @param key   The header key.
         * @param value The header value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addDefaultResponseHeader(final String key, final String value) {
            configuration.defaultResponseHeaders.put(key, value);
            return this;
        }

        /**
         * Adds all default response headers to the configuration being built,
         * where neither the key nor the value is empty or null. Any existing
         * keys will be overwritten. If a corresponding header exists in a mock
         * response definition, then that header will be honored instead.
         *
         * @param responseHeaders The headers to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addDefaultResponseHeaders(final Map<String, String> responseHeaders) {
            configuration.defaultResponseHeaders.putAll(responseHeaders);
            return this;
        }

        /**
         * Adds a default response setting to the configuration being built. Any
         * existing keys will be overwritten.
         *
         * @param key   The setting key.
         * @param value The setting value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addDefaultResponseSetting(final String key, final String value) {
            configuration.defaultResponseSettings.put(key, value);
            return this;
        }

        /**
         * Adds all default response settings to the configuration being built
         * where neither the key nor the value is empty or null. Any existing
         * keys will be overwritten.
         *
         * @param settings The settings to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addDefaultResponseSettings(final Map<String, String> settings) {
            configuration.defaultResponseSettings.putAll(settings);
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
         * Sets the real/mock context transformation helper to use when relaying
         * a request to a real server and introducing a real response to the
         * mock context.
         *
         * @param helper The mock transformation helper. May be null.
         * @return This builder object, allowing chaining of method calls.
         */
        public Builder setTransformationHelper(final Atlantis.TransformationHelper helper) {
            configuration.transformationHelper = helper;
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
    private List<MockRequest> requests = null;
    private HeaderManager defaultResponseHeaders = null;
    private SettingsManager defaultResponseSettings = null;
    private transient MockRequest.Filter requestFilter = null;
    private transient Atlantis.TransformationHelper transformationHelper = null;


    Configuration() {
        requests = new ArrayList<>();
        defaultResponseHeaders = new HeaderManager();
        defaultResponseSettings = new SettingsManager();
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
        return Collections.unmodifiableList(requests);
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
     * Returns the atlantis transformation helper for this configuration.
     *
     * @return A transport helper implementation. May be null if no
     * transformation helper has been defined.
     */
    Atlantis.TransformationHelper transformationHelper() {
        return transformationHelper;
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

        return filter.findRequest(meta.method(), meta.url(), meta.headers(), requests);
    }

    /**
     * Returns the default response behavior settings. If a corresponding
     * setting is found in the mock response definition, then that setting will
     * be honored instead.
     *
     * @return The response settings.
     */
    SettingsManager defaultResponseSettings() {
        return defaultResponseSettings;
    }

    /**
     * Returns the default response headers. If a corresponding header is found
     * in the mock response definition, then that header will be honored
     * instead.
     *
     * @return The default response headers.
     */
    HeaderManager defaultResponseHeaders() {
        return defaultResponseHeaders;
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
        if (!requests.contains(mockRequest))
            requests.add(mockRequest);
    }
}
