package com.echsylon.atlantis;

import com.echsylon.atlantis.filter.DefaultRequestFilter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
         * Creates a new builder based on the provided configuration object.
         *
         * @param source The configuration to initialize this builder with.
         */
        public Builder(Configuration source) {
            configuration = new Configuration();
            if (source != null) {
                configuration.requests.addAll(source.requests);
                configuration.headerManager.add(source.headerManager.getAllAsMultiMap());
                configuration.settingsManager.set(source.settingsManager.getAllAsMap());
            }
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
         * Adds any default response headers with an absent key to the
         * configuration being built. This method is meant for internal use
         * only.
         *
         * @param headers The headers.
         * @return This builder instance, allowing chaining of method calls.
         */
        Builder addDefaultResponseHeadersIfKeyAbsent(final Map<String, List<String>> headers) {
            configuration.headerManager.addIfKeyAbsent(headers);
            return this;
        }

        /**
         * Adds any default settings with an absent key to the configuration
         * being built. This method is meant for internal use only.
         *
         * @param settings The new settings manager. Null is ignored
         * @return This builder instance, allowing chaining of method calls.
         */
        Builder addSettingsIfKeyAbsent(final Map<String, String> settings) {
            configuration.settingsManager.setIfAbsent(settings);
            return this;
        }

        /**
         * Adds a default response header to the configuration being build,
         * replacing any and all existing values for the same key.
         *
         * @param key   The header key.
         * @param value The new header value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setDefaultResponseHeader(final String key, final String value) {
            configuration.headerManager.set(key, value);
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
            configuration.headerManager.add(key, value);
            return this;
        }

        /**
         * Adds all non-empty default response headers to the configuration
         * being built.
         *
         * @param key    The header key.
         * @param values The header values.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addDefaultResponseHeaders(final String key, final List<String> values) {
            configuration.headerManager.add(key, values);
            return this;
        }

        /**
         * Adds all non-empty default response headers to the configuration
         * being built.
         *
         * @param key    The header key.
         * @param values The header values.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addDefaultResponseHeaders(final String key, final String... values) {
            configuration.headerManager.add(key, Arrays.asList(values));
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
        public Builder addDefaultResponseHeaders(final Map<String, List<String>> responseHeaders) {
            configuration.headerManager.add(responseHeaders);
            return this;
        }

        /**
         * Adds a default response setting to the configuration being built. Any
         * existing keys will be overwritten.
         *
         * @param key   The setting key.
         * @param value The setting value.
         * @return This builder instance, allowing chaining of method calls.
         * @deprecated Use {@link #setSetting(String, String)} instead.
         * Planned to be removed in Atlantis 3.0
         */
        public Builder addDefaultResponseSetting(final String key, final String value) {
            return setSetting(key, value);
        }

        /**
         * Adds all default response settings to the configuration being built
         * where neither the key nor the value is empty or null. Any existing
         * keys will be overwritten.
         *
         * @param settings The settings to add.
         * @return This builder instance, allowing chaining of method calls.
         * @deprecated Use {@link #setSettings(Map)} instead. Planned to
         * be removed in Atlantis 3.0
         */
        public Builder addDefaultResponseSettings(final Map<String, String> settings) {
            return setSettings(settings);
        }

        /**
         * Sets the fallback base url to hit when no mocked request was found
         * and Atlantis is configured to fall back to real world responses.
         *
         * @param realBaseUrl The real-world base URL, including scheme.
         * @return This builder object, allowing chaining of method calls.
         * @deprecated Use {@link #setSetting(String, String)} with
         * {@link SettingsManager#FALLBACK_BASE_URL} as key instead. Planned to
         * be removed in Atlantis 3.0
         */
        public Builder setFallbackBaseUrl(final String realBaseUrl) {
            configuration.settingsManager.set(SettingsManager.FALLBACK_BASE_URL, realBaseUrl);
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
        public Builder setSetting(final String key, final String value) {
            configuration.settingsManager.set(key, value);
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
        public Builder setSettings(final Map<String, String> settings) {
            configuration.settingsManager.set(settings);
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
            configuration.settingsManager.set(
                    SettingsManager.REQUEST_FILTER,
                    filter.getClass().getCanonicalName());
            return this;
        }

        /**
         * Sets a helper implementation that manages token parsing and state
         * keeping based on request data.
         *
         * @param tokenHelper The new token helper implementation.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setTokenHelper(final Atlantis.TokenHelper tokenHelper) {
            configuration.settingsManager.set(
                    SettingsManager.TOKEN_HELPER,
                    tokenHelper.getClass().getCanonicalName());
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
            configuration.settingsManager.set(
                    SettingsManager.TRANSFORMATION_HELPER,
                    helper.getClass().getCanonicalName());
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


    private List<MockRequest> requests = null;
    private HeaderManager headerManager = null;
    private SettingsManager settingsManager = null;


    Configuration() {
        requests = new ArrayList<>();
        headerManager = new HeaderManager();
        settingsManager = new SettingsManager();
    }

    /**
     * Returns the fallback base url for this configuration. If given and
     * Atlantis is configured to fall back to real world responses, this is the
     * suggested real world base URL to target (replacing "localhost") if no
     * configuration was found for a request.
     *
     * @return The fallback base url or null.
     * @deprecated Use {@link #settingsManager()} instead. Planned to be
     * removed in Atlantis 3.0
     */
    public String fallbackBaseUrl() {
        return settingsManager.fallbackBaseUrl();
    }

    /**
     * Returns the request filter of this configuration.
     *
     * @return The request filter or null.
     */
    public MockRequest.Filter requestFilter() {
        return settingsManager.requestFilter();
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
     * Returns the header manager managing the default response headers.
     *
     * @return The default response header manager. Never null.
     */
    public HeaderManager defaultResponseHeaderManager() {
        return headerManager;
    }

    /**
     * Returns the default settings manager that constrain mock request and
     * response behavior.
     *
     * @return The default settings manager. Never null.
     */
    SettingsManager settingsManager() {
        return settingsManager;
    }

    /**
     * Returns the current token helper implementation. See {@link
     * Atlantis.TokenHelper} for more info on what this is.
     *
     * @return The token helper.
     */
    Atlantis.TokenHelper tokenHelper() {
        return settingsManager.tokenHelper();
    }

    /**
     * Returns the atlantis transformation helper for this configuration.
     *
     * @return A transport helper implementation. May be null if no
     * transformation helper has been defined.
     */
    Atlantis.TransformationHelper transformationHelper() {
        return settingsManager.transformationHelper();
    }

    /**
     * Returns a suitable request template for the provided parameters.
     *
     * @return The request filter. May be null.
     */
    MockRequest findRequest(final Meta meta) {
        MockRequest.Filter filter = requestFilter();
        if (filter == null)
            filter = new DefaultRequestFilter();

        return filter.findRequest(meta.method(), meta.url(), meta.headerManager().getAllAsMap(), requests);
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
