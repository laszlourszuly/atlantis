package com.echsylon.atlantis;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import okio.Okio;
import okio.Source;

import static com.echsylon.atlantis.LogUtils.info;
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
     * response. Implementing classes are responsible for picking a response
     * from a given list of available responses. Any state machine etc is also
     * in the scope of the implementing class.
     */
    public interface Filter {

        /**
         * Returns a mocked response of choice. May be null, in which case the
         * calling logic is responsible for deciding what response to serve.
         *
         * @param mockResponses All available response to pick a candidate from.
         *                      Null and empty lists are acceptable.
         * @return The response candidate.
         */
        MockResponse findResponse(final List<MockResponse> mockResponses);
    }

    /**
     * This interface describes the mandatory feature set to provide a data
     * stream for content described in the given text.
     */
    public interface SourceHelper {

        /**
         * Returns a stream from which the response body content can be read.
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
         * Creates a new builder based on an uninitialized response object.
         */
        public Builder() {
            mockResponse = new MockResponse();
        }

        /**
         * Adds a header to the response being built. Any existing keys will be
         * overwritten.
         *
         * @param key   The header key.
         * @param value The header value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeader(final String key, final String value) {
            mockResponse.headers.put(key, value);
            return this;
        }

        /**
         * Adds all headers to the response being built where neither the key
         * nor the value is empty or null. Any existing keys will be
         * overwritten.
         *
         * @param headers The headers to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeaders(final Map<String, String> headers) {
            mockResponse.headers.putAll(headers);
            return this;
        }

        /**
         * Adds a setting to the response being built. Any existing keys will be
         * overwritten.
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
         * Adds all settings to the response being built where neither the key
         * nor the value is empty or null. Any existing keys will be
         * overwritten.
         *
         * @param settings The settings to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addSettings(final Map<String, String> settings) {
            mockResponse.settings.putAll(settings);
            return this;
        }

        /**
         * Sets the status of the response being built. Doesn't validate neither
         * the given status code nor the phrase. It's up to the caller to ensure
         * they match and make sense in the given context.
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
         * @param string The new response body content.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final String string) {
            mockResponse.text = string;
            // Fallback to default source helper
            // (provided by parent infrastructure).
            return this;
        }

        /**
         * Sets a byte array as the body content of the mock response being
         * built.
         *
         * @param bytes The new response body content.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final byte[] bytes) {
            mockResponse.text = null;
            mockResponse.sourceHelper = text -> Okio.source(new ByteArrayInputStream(bytes));
            return this;
        }

        /**
         * Sets a file that will provide the body content of the mock response
         * being built.
         *
         * @param file The new response body content source.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final File file) {
            mockResponse.text = null;
            mockResponse.sourceHelper = text -> {
                try {
                    return Okio.source(file);
                } catch (FileNotFoundException e) {
                    info(e, "Couldn't open source: %s", file.getAbsolutePath());
                    return null;
                }
            };
            return this;
        }

        /**
         * Sets an InputStream that will provide the body content of the mock
         * response being built.
         *
         * @param inputStream The new response body content source.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final InputStream inputStream) {
            mockResponse.text = null;
            mockResponse.sourceHelper = text -> Okio.source(inputStream);
            return this;
        }

        /**
         * Returns a sealed response object which can not be further built on.
         *
         * @return The final response object.
         */
        public MockResponse build() {
            return mockResponse;
        }
    }

    private String text = null;
    private Integer code = null;
    private String phrase = null;
    private HeaderManager headers = null;
    private SettingsManager settings = null;
    private transient SourceHelper sourceHelper = null;


    MockResponse() {
        headers = new HeaderManager();
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
     * Returns an unmodifiable map of the mocked response headers. NOTE! that
     * {@code Atlantis} may internally decide to add, remove or overwrite some
     * headers, depending on the characteristics of the response itself.
     * "Content-Length" would be an example of this.
     *
     * @return The unmodifiable response headers map as per definition in {@link
     * Collections#unmodifiableMap(Map)}.
     */
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Returns the body content description of this mock response. NOTE! that
     * this isn't necessarily the actual data, but rather a description of how
     * to get hold of the data, e.g. "file://path/to/file.json" is a perfectly
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
     * @return The response body.
     */
    Source source() {
        return sourceHelper != null ?
                sourceHelper.open(text) :
                null;
    }

    /**
     * Returns the response behavior settings.
     *
     * @return The response settings.
     */
    SettingsManager settings() {
        return settings;
    }

    /**
     * Sets the source helper implementation that will help open a data stream
     * for any response body content.
     *
     * @param sourceHelper The source open helper.
     */
    void setSourceHelperIfAbsent(final SourceHelper sourceHelper) {
        if (this.sourceHelper == null)
            this.sourceHelper = sourceHelper;
    }

    /**
     * Adds any default headers to the mock response if not already exists.
     *
     * @param defaultHeaders The default headers map.
     */
    void addHeadersIfAbsent(final Map<String, String> defaultHeaders) {
        if (notEmpty(defaultHeaders))
            for (Map.Entry<String, String> entry : defaultHeaders.entrySet())
                headers.putIfAbsent(entry.getKey(), entry.getValue());
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
     * Returns a boolean flag indicating whether there is an "Expect" header
     * with a "100-continue" value.
     *
     * @return Boolean true if there is an "Expect" header and a corresponding
     * "100-Continue" value, false otherwise.
     */
    boolean isExpectedToContinue() {
        return headers.isExpectedToContinue();
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
