package com.echsylon.atlantis;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okio.Okio;
import okio.Source;

import static com.echsylon.atlantis.LogUtils.info;
import static com.echsylon.atlantis.Utils.getNative;
import static com.echsylon.atlantis.Utils.getNonNull;

/**
 * This class contains the full description of a mock mockResponse.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MockResponse {

    /**
     * This interface describes the mandatory feature set to provide a mocked
     * mockResponse. Implementing classes are responsible for picking a
     * mockResponse from a given list of available responses. Any state machine
     * etc is also in the scope of the implementing class.
     */
    public interface Filter {

        /**
         * Returns a mocked mockResponse of choice. May be null, in which case
         * the calling logic is responsible for deciding what mockResponse to
         * serve.
         *
         * @param mockResponses All available mockResponse to pick a candidate
         *                      from. The list may be empty but will never be
         *                      null.
         * @return The mockResponse candidate.
         */
        MockResponse findResponse(final List<MockResponse> mockResponses);
    }

    /**
     * This interface describes the mandatory feature set to provide a data
     * stream for content described in the given text.
     */
    public interface SourceHelper {

        /**
         * Returns a stream from which the mockResponse body content can be
         * read.
         *
         * @param text The description of the content to stream.
         * @return A data stream or null.
         */
        Source open(final String text);
    }


    /**
     * This class offers means of building a mocked mockResponse configuration
     * directly from code (as opposed to configure one in a JSON file).
     */
    public static final class Builder {
        private final MockResponse mockResponse;

        /**
         * Creates a new builder based on an uninitialized mockResponse object.
         */
        public Builder() {
            mockResponse = new MockResponse();
        }

        /**
         * Adds a header to the mockResponse being built.
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
         * Adds all headers to the mockResponse being built where neither the
         * key nor the value is empty or null.
         *
         * @param headers The headers to add.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder addHeaders(final Map<String, String> headers) {
            mockResponse.headers.putAll(headers);
            return this;
        }

        /**
         * Sets the status of the mockResponse being built. Doesn't validate
         * neither the given status code nor the phrase. It's up to the caller
         * to ensure they match and make sense in the given context.
         *
         * @param code   The new HTTP status code.
         * @param phrase The corresponding human readable status text (e.g.
         *               "OK", "Not found", etc).
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setStatus(final int code, final String phrase) {
            mockResponse.status = code;
            mockResponse.phrase = phrase;
            return this;
        }

        /**
         * Sets a byte array as the body content of the mockResponse being
         * built.
         *
         * @param bytes The new mockResponse body content.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final byte[] bytes) {
            mockResponse.sourceHelper = text -> Okio.source(new ByteArrayInputStream(bytes));
            return this;
        }

        /**
         * Sets a string as the body content of the mockResponse being built.
         *
         * @param string The new mockResponse body content.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final String string) {
            mockResponse.sourceHelper = text -> {
                try {
                    return Okio.source(new ByteArrayInputStream(string.getBytes("UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    info(e, "Couldn't open UTF-8 string source: %s", string);
                    return null;
                }
            };
            return this;
        }

        /**
         * Sets a file (the content of the file to be more specific) as the body
         * content of the mockResponse being built.
         *
         * @param file The new mockResponse body content.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final File file) {
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
         * Sets an InputStream (the data provided by the input stream to be more
         * specific) as the body content of the mockResponse being built.
         *
         * @param inputStream The new mockResponse body content.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setBody(final InputStream inputStream) {
            mockResponse.sourceHelper = text -> Okio.source(inputStream);
            return this;
        }

        /**
         * Sets the default delay for the mockResponse being built, disabling
         * the max delay.
         *
         * @param milliseconds The new amount of milliseconds to delay this
         *                     mockResponse.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setDelay(long milliseconds) {
            mockResponse.delay = milliseconds;
            mockResponse.maxDelay = null;
            return this;
        }

        /**
         * Sets the random delay metrics for the mockResponse being built.
         *
         * @param milliseconds    The minimum amount of random milliseconds to
         *                        delay this mockResponse.
         * @param maxMilliseconds The maximum amount of random milliseconds to
         *                        delay this mockResponse.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder setDelay(long milliseconds, long maxMilliseconds) {
            mockResponse.delay = milliseconds;
            mockResponse.maxDelay = maxMilliseconds;
            return this;
        }

        /**
         * Returns a sealed mockResponse object which can not be further built
         * on.
         *
         * @return The final mockResponse object.
         */
        public MockResponse build() {
            return mockResponse;
        }
    }


    private Integer status = null;
    private String phrase = null;
    private String text = null;
    private Long delay = null;
    private Long maxDelay = null;
    private Settings settings = null;
    private HeaderManager headers = null;
    private transient SourceHelper sourceHelper = null;


    MockResponse() {
        headers = new HeaderManager();
    }

    /**
     * Returns the mocked mockResponse code.
     *
     * @return The mocked HTTP mockResponse code.
     */
    public int code() {
        return getNative(status, 0);
    }

    /**
     * Returns the mocked mockResponse phrase.
     *
     * @return The mocked HTTP mockResponse phrase.
     */
    public String phrase() {
        return getNonNull(phrase, "");
    }

    /**
     * Returns an unmodifiable map of the mocked mockResponse headers. NOTE!
     * that {@code Atlantis} may internally decide to add, remove or overwrite
     * some headers, depending on the characteristics of the mockResponse
     * itself. "Content-Length" would be an example of this.
     *
     * @return The unmodifiable mockResponse headers map as per definition in
     * {@link Collections#unmodifiableMap(Map)}.
     */
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Returns the body content description of this mock mockResponse. NOTE!
     * that this isn't necessarily the actual data, but rather a description of
     * how to get hold of the data, e.g. "file://path/to/file.json" is a
     * perfectly valid body content descriptor.
     *
     * @return A string that describes the mock mockResponse body content.
     */
    public String body() {
        return text;
    }

    /**
     * Returns the mocked mockResponse body stream.
     *
     * @return The mockResponse body.
     */
    Source content() {
        return sourceHelper != null ?
                sourceHelper.open(text) :
                null;
    }

    /**
     * Returns the amount of milliseconds this mockResponse should be delayed
     * before the mockResponse body is sent to the waiting HTTP client. The
     * algorithm as of which the "delay" and "maxDelay" parameters are
     * interpreted is:
     * <p>
     * If only the "delay" variable is set, then that value is returned.
     * <p>
     * If both "delay" and "maxDelay" is given, then a different random number
     * between those values is returned every time this method is called.
     * <p>
     * If only "maxDelay" is given then a different random number between zero
     * (0) and "maxDelay" is returned every time this method is called.
     * <p>
     * If neither "delay" nor "maxDelay" is given, then zero (0) is returned.
     * <p>
     * If "maxDelay" is less than "delay" then "delay" is returned.
     *
     * @return A number of milliseconds. Never less than zero (0). Zero means no
     * delay.
     */
    long delay() {
        if (maxDelay == null || maxDelay.equals(delay))
            return Math.max(0L, Utils.getNative(delay, 0L));

        long min = Math.max(0L, Utils.getNative(delay, 0L));
        long max = Math.max(0L, Utils.getNative(maxDelay, 0L));

        return max > min ?
                new Random().nextInt((int) (max - min)) + min :
                min;
    }

    /**
     * Returns the mockResponse behavior settings.
     *
     * @return The mockResponse settings.
     */
    Settings settings() {
        return settings == null ?
                new Settings() :
                settings;
    }

    /**
     * Sets the source helper implementation that will help open a data stream
     * for any mockResponse body content.
     *
     * @param sourceHelper The source open helper.
     */
    void setSourceHelper(final SourceHelper sourceHelper) {
        this.sourceHelper = sourceHelper;
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
