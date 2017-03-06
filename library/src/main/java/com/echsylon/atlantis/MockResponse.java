package com.echsylon.atlantis;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import okio.BufferedSource;
import okio.Okio;
import okio.Source;

import static com.echsylon.atlantis.LogUtils.info;
import static com.echsylon.atlantis.Utils.closeSilently;
import static com.echsylon.atlantis.Utils.getNative;
import static com.echsylon.atlantis.Utils.getNonNull;

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
         * @param source The description of the content body source.
         * @return A data stream or null.
         */
        Source open(final byte[] source);
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
         * Sets the settings manager of the mock response being built. This
         * method is meant for internal use only.
         *
         * @param settingsManager The new settings manager. Null is handled
         *                        gracefully.
         * @return This builder instance, allowing chaining of method calls.
         */
        Builder setSettingsManager(final SettingsManager settingsManager) {
            mockResponse.settingsManager = settingsManager == null ?
                    new SettingsManager() :
                    settingsManager;
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
            mockResponse.settingsManager.set(key, value);
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
            mockResponse.settingsManager.set(settings);
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
            mockResponse.source = string.getBytes();
            return this;
        }

        /**
         * Sets a byte array as the body content fo the mock response being
         * built.
         *
         * @param bytes The new mock response body content.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final byte[] bytes) {
            mockResponse.source = bytes;
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

    private Integer code = null;
    private String phrase = null;
    private byte[] source = null;
    private transient SourceHelper sourceHelper = null;
    private transient HeaderManager headerManager = null;
    private transient SettingsManager settingsManager = null;


    MockResponse() {
        headerManager = new HeaderManager();
        settingsManager = new SettingsManager();
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
     * Tries to return the body as a string.
     *
     * @return The response body as a byte array. May be empty, never null.
     */
    public byte[] body() {
        BufferedSource bufferedSource = null;
        try {
            bufferedSource = Okio.buffer(sourceHelper.open(source));
            return bufferedSource.readByteArray();
        } catch (NullPointerException | IOException e) {
            info(e, "Couldn't deliver mock response body: %s", new String(source));
            return new byte[0];
        } finally {
            closeSilently(bufferedSource);
        }
    }

    /**
     * @return The body as a byte array.
     */
    public byte[] bodyAsByteArray() {
        return source;
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
     * Sets the source reader if not already set.
     *
     * @param helper The source helper used to open the data source with.
     */
    void setSourceHelperIfAbsent(final SourceHelper helper) {
        if (sourceHelper == null)
            sourceHelper = helper;
    }

    /**
     * Returns the content body description.
     *
     * @return The content body description.
     */
    String source() {
        return new String(source);
    }

    /**
     * Returns the settings manager.
     *
     * @return The response settings manager. Never null.
     */
    SettingsManager settingsManager() {
        return settingsManager;
    }
}
