package com.echsylon.atlantis;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import okio.Okio;
import okio.Source;

import static com.echsylon.atlantis.Utils.getNative;
import static com.echsylon.atlantis.Utils.getNonNull;
import static com.echsylon.atlantis.Utils.notEmpty;

/**
 * This class contains the full description of a mock response.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MockResponse {

    /**
     * This interface describes the mandatory feature set to provide a mocked
     * response. Implementing classes are responsible for picking a mock
     * response from a given list of available responses. Any state machine etc
     * is also in the scope of the implementing class.
     */
    public interface Filter {

        /**
         * Returns a mocked response of choice. May be null, in which case the
         * calling logic is responsible for deciding what to do.
         *
         * @param mockResponses All available mock responses to pick a candidate
         *                      from. Null and empty lists are acceptable.
         * @return The mock response candidate.
         */
        MockResponse findResponse(final List<MockResponse> mockResponses);
    }

    /**
     * This interface describes the mandatory feature set to provide a data
     * stream for content described in the given text.
     */
    public interface SourceHelper {

        /**
         * Returns a content stream from which the mock response body content
         * can be read.
         *
         * @param text The description of the content to stream.
         * @return A data stream or null.
         */
        Source open(final String text);
    }

    /**
     * This class offers means of building a mocked response configuration
     * directly from code (as opposed to configure one in a JSON file).
     */
    public static final class Builder {
        private final MockResponse mockResponse;

        /**
         * Creates a new builder based on an empty mock response object.
         */
        public Builder() {
            this(new MockResponse());
        }

        /**
         * Creates a new builder based on the provided mock response object.
         *
         * @param source The mock response to initialize this builder with.
         */
        public Builder(final MockResponse source) {
            mockResponse = source != null ? source : new MockResponse();
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
            mockResponse.headerManager = headerManager == null ?
                    new HeaderManager() :
                    headerManager;
            return this;
        }

        /**
         * Adds a header to the mock response being build, replacing any and all
         * existing values for the same key.
         *
         * @param key   The header key.
         * @param value The new header value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setHeader(final String key, final String value) {
            mockResponse.headerManager.set(key, value);
            return this;
        }

        /**
         * Adds a header to the mock response being built.
         *
         * @param key   The header key.
         * @param value The header value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeader(final String key, final String value) {
            mockResponse.headerManager.add(key, value);
            return this;
        }

        /**
         * Adds all non-empty header keys and values to the mock response being
         * built.
         *
         * @param key    The header key.
         * @param values The header values.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeaders(final String key, final List<String> values) {
            mockResponse.headerManager.add(key, values);
            return this;
        }

        /**
         * Adds all non-empty header keys and values to the mock response being
         * built.
         *
         * @param key    The header key.
         * @param values The header values.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeaders(final String key, final String... values) {
            mockResponse.headerManager.add(key, Arrays.asList(values));
            return this;
        }

        /**
         * Adds all non-empty header keys and values to the mock response being
         * built.
         *
         * @param headers The headers to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeaders(final Map<String, List<String>> headers) {
            mockResponse.headerManager.add(headers);
            return this;
        }

        /**
         * Adds a setting to the mock response being built. Any existing value
         * with the same key will be overwritten.
         *
         * @param key   The setting key.
         * @param value The setting value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addSetting(final String key, final String value) {
            mockResponse.settings.put(key, value);
            return this;
        }

        /**
         * Adds all non-empty settings to the mock response being built. Any
         * existing values with the same keys will be overwritten.
         *
         * @param settings The settings to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addSettings(final Map<String, String> settings) {
            mockResponse.settings.putAll(settings);
            return this;
        }

        /**
         * Sets the status of the mock response being built. Doesn't validate
         * neither the given status code nor the phrase. It's up to the caller
         * to ensure they match and make sense in the given context.
         *
         * @param code   The new HTTP status code.
         * @param phrase The corresponding human readable status text (e.g.
         *               "OK", "Not found", etc).
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setStatus(final int code, final String phrase) {
            mockResponse.code = code;
            mockResponse.phrase = phrase;
            return this;
        }

        /**
         * Sets a string as the body content of the mock response being built.
         *
         * @param string The new mock response body content.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final String string) {
            mockResponse.sourceHelper = null;
            mockResponse.text = string;
            return this;
        }

        /**
         * Sets a byte array as the body content of the mock response being
         * built.
         *
         * @param bytes The new mock response body content.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final byte[] bytes) {
            mockResponse.sourceHelper = ignored -> Okio.source(new ByteArrayInputStream(bytes));
            mockResponse.text = null;
            return this;
        }

        /**
         * Sets a file that will provide the body content of the mock response
         * being built.
         *
         * @param file The new mock response body content source.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final File file) {
            mockResponse.sourceHelper = null;
            mockResponse.text = file != null ?
                    "file://" + file.getAbsolutePath() :
                    null;
            return this;
        }

        /**
         * Sets an InputStream that will provide the body content of the mock
         * response being built.
         *
         * @param inputStream The new mock response body content source.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final InputStream inputStream) {
            mockResponse.sourceHelper = ignored -> Okio.source(inputStream);
            mockResponse.text = null;
            return this;
        }

        /**
         * Returns a sealed mock response object which can not be further built
         * on.
         *
         * @return The final mock response object.
         */
        public MockResponse build() {
            return mockResponse;
        }
    }

    private String text = null;
    private Integer code = null;
    private String phrase = null;
    private SettingsManager settings = null;
    private transient SourceHelper sourceHelper = null;
    private transient HeaderManager headerManager = null;


    MockResponse() {
        headerManager = new HeaderManager();
        settings = new SettingsManager();
    }

    /**
     * Returns the mocked response code.
     *
     * @return The mocked HTTP response code.
     */
    public int code() {
        return getNative(code, 0);
    }

    /**
     * Returns the mocked response phrase.
     *
     * @return The mocked HTTP response phrase.
     */
    public String phrase() {
        return getNonNull(phrase, "");
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
     * Returns the body content description of this mock response. NOTE! that
     * this isn't necessarily the actual data, but maybe a description of how to
     * get hold of the data, e.g. "file://path/to/file.json" is a perfectly
     * valid body content descriptor.
     *
     * @return A string that describes the mock response body content.
     */
    public String body() {
        return text;
    }

    /**
     * Returns the mocked response body stream.
     *
     * @return The mock response body content stream.
     */
    Source source() {
        return sourceHelper != null ?
                sourceHelper.open(text) :
                null;
    }

    /**
     * Returns the mock response behavior settings.
     *
     * @return The mock response settings.
     */
    SettingsManager settings() {
        return settings;
    }

    /**
     * Sets the source helper implementation that will help open a data stream
     * for any mock response body content.
     *
     * @param sourceHelper The source open helper.
     */
    void setSourceHelperIfAbsent(final SourceHelper sourceHelper) {
        if (this.sourceHelper == null)
            this.sourceHelper = sourceHelper;
    }

    /**
     * Adds any default settings to the mock response if not already exists.
     *
     * @param defaultSettings The default headers map.
     */
    void addSettingsIfAbsent(final Map<String, String> defaultSettings) {
        if (notEmpty(defaultSettings))
            for (Map.Entry<String, String> entry : defaultSettings.entrySet())
                settings.putIfAbsent(entry.getKey(), entry.getValue());
    }
}
