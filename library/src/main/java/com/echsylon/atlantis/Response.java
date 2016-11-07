package com.echsylon.atlantis;

import android.content.Context;

import com.echsylon.atlantis.internal.Utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.echsylon.atlantis.internal.Utils.notAnyEmpty;

/**
 * This class contains all the data required by {@link com.echsylon.atlantis.Atlantis
 * Atlantis} to serve a response.
 */
@SuppressWarnings("WeakerAccess")
public class Response extends HttpEntity implements Serializable {
    private static final String ASSET_SCHEME = "asset://";
    private static final String FILE_SCHEME = "file://";
    public static final Response EMPTY = new Response();

    /**
     * This interface describes the features for filtering out a particular
     * response based on the implemented logic.
     */
    public interface Filter {

        /**
         * Returns the next response based on internal state and logic.
         *
         * @param request   The request to find a response for.
         * @param responses All available responses.
         * @return The filtered response or null.
         */
        Response getResponse(Request request, List<Response> responses);

    }

    /**
     * This class offers means of building a mocked response configuration
     * directly from code (as opposed to configure one in a JSON asset).
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder extends Response {

        /**
         * Adds a header to the response being built. Doesn't add anything if
         * either {@code key} or {@code value} is empty (null pointers are
         * considered empty).
         *
         * @param key   The header key.
         * @param value The header value.
         * @return This buildable response instance, allowing chaining of method
         * calls.
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
         * @return This buildable response instance, allowing chaining of method
         * calls.
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
         * Sets the response code of this response. Doesn't validate neither the
         * given status code nor the name. It's up to the caller to ensure they
         * make sense in the given context.
         *
         * @param code The new HTTP status code.
         * @param name The corresponding human readable status text (e.g. "OK",
         *             "Not found", etc).
         * @return This buildable response instance, allowing chaining of method
         * calls.
         */
        public Builder withStatus(int code, String name) {
            if (this.responseCode == null)
                this.responseCode = new Code();

            this.responseCode.code = code;
            this.responseCode.name = name;
            return this;
        }

        /**
         * Sets the MIME type of this response. This method doesn't validate the
         * input.
         *
         * @param mimeType The new MIME type.
         * @return This buildable response instance, allowing chaining of method
         * calls.
         */
        public Builder withMimeType(String mimeType) {
            this.mime = mimeType;
            return this;
        }

        /**
         * Sets the plain text content of this response. This method doesn't
         * validate the input or that it matches any set MIME type.
         *
         * @param content The new plain text content.
         * @return This buildable response instance, allowing chaining of method
         * calls.
         */
        public Builder withContent(String content) {
            this.text = content;
            return this;
        }

        /**
         * Sets the asset resource path of this response.
         *
         * @param assetPath The new asset path.
         * @return This buildable response instance, allowing chaining of method
         * calls.
         */
        public Builder withAsset(String assetPath) {
            this.asset = assetPath;
            return this;
        }

        /**
         * Sets the asset resource bytes of this response.
         *
         * @param assetBytes The new asset as a byte array.
         * @return This buildable response instance, allowing chaining of method
         * calls.
         */
        public Builder withAsset(byte[] assetBytes) {
            this.assetBytes = assetBytes;
            return this;
        }

        /**
         * Sets the default delay for this response, disabling the max delay. If
         * the new default delay is less than or equal to zero, then the delay
         * feature is reset.
         *
         * @param milliseconds The new amount of milliseconds to delay this
         *                     response.
         * @return This buildable response instance, allowing chaining of method
         * calls.
         */
        public Builder withDelay(int milliseconds) {
            this.delay = null;
            this.maxDelay = null;

            if (milliseconds > 0) {
                this.delay = milliseconds;
            }

            return this;
        }

        /**
         * Sets the random delay metrics for this response. If the new min delay
         * is less than or equal to zero, or the max delay is less than the min
         * delay, then the delay feature is reset.
         *
         * @param milliseconds    The minimum amount of random milliseconds to
         *                        delay this response.
         * @param maxMilliseconds The maximum amount of random milliseconds to
         *                        delay this response.
         * @return This buildable response instance, allowing chaining of method
         * calls.
         */
        public Builder withDelay(int milliseconds, int maxMilliseconds) {
            this.delay = null;
            this.maxDelay = null;

            if (milliseconds > 0 && maxMilliseconds >= milliseconds) {
                this.delay = milliseconds;
                this.maxDelay = maxMilliseconds > milliseconds ?
                        maxMilliseconds :
                        null;
            }

            return this;
        }

        /**
         * Returns a sealed response object which can not be further built on.
         *
         * @return The final response object.
         */
        public Response build() {
            return this;
        }

    }


    /**
     * This class represents a status code on a response.
     */
    private static final class Code {
        private Integer code = null;
        private String name = null;

        // Intentionally hidden constructor.
        private Code() {
        }
    }

    // JSON API
    protected Code responseCode = null;
    protected String mime = null;
    protected String text = null;
    protected String asset = null;
    protected Integer delay = null;
    protected Integer maxDelay = null;

    // Part of extra feature offered by the Builder.
    // This field should be ignored during serialization, hence transient.
    protected transient byte[] assetBytes = null;

    // Intentionally hidden constructor.
    protected Response() {
    }

    /**
     * Returns the HTTP status code of the response.
     *
     * @return The numeric HTTP status code.
     */
    @SuppressWarnings("ConstantConditions")
    public int statusCode() {
        return responseCode != null ?
                Utils.getNative(responseCode.code, 0) :
                0;
    }

    /**
     * Returns the HTTP status name of the response.
     *
     * @return The human readable name of the HTTP status.
     */
    @SuppressWarnings("ConstantConditions")
    public String statusName() {
        return responseCode != null ?
                responseCode.name :
                null;
    }

    /**
     * Returns the MIME type of the response.
     *
     * @return The MIME type. May be null.
     */
    public String mimeType() {
        return mime;
    }

    /**
     * Returns the response body string.
     *
     * @return The content. May be null.
     */
    public String content() {
        return text;
    }

    /**
     * Returns the response asset content as an array of bytes. It's up to the
     * caller to decide how to further process the result.
     * <p>
     * NOTE! This method will read data from disk on the calling thread.
     *
     * @param context The context the resource asset is to be opened from.
     * @return The asset byte array. May be empty but never null.
     * @throws IOException If for some reason the asset file could not be read.
     */
    public byte[] asset(Context context) throws IOException {
        if (assetBytes != null)
            return assetBytes;

        if (asset == null)
            return new byte[0];

        if (asset.startsWith(ASSET_SCHEME))
            return Utils.readAsset(context, asset);

        if (asset.startsWith(FILE_SCHEME))
            return Utils.readFile(new File(asset.substring(FILE_SCHEME.length())));

        return new byte[0];
    }

    /**
     * Returns the amount of milliseconds to wait before serving this particular
     * response.
     * <p>
     * If only the "delay" variable exists, then that value is returned.
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
     * @return Number of milliseconds. Never less than zero (0). Zero means no
     * delay.
     */
    public long delay() {
        if (maxDelay == null || maxDelay.equals(delay))
            return Math.max(0, Utils.getNative(delay, 0));

        int min = Math.max(0, Utils.getNative(delay, 0));
        int max = Math.max(0, Utils.getNative(maxDelay, 0));
        return max > min ?
                new Random().nextInt(max - min) + min :
                min;
    }

    /**
     * Returns a flag telling whether the response has a non-empty content
     * text.
     *
     * @return Boolean true if the content text is not a null pointer or an
     * empty string, otherwise false.
     */
    public boolean hasContent() {
        return Utils.notEmpty(text);
    }

    /**
     * Returns a flag telling whether the response has a valid, non-empty asset
     * file pointer. This flag doesn't say anything about the actual content in
     * such an asset file which very well may even be empty or invalid.
     *
     * @return Boolean true if the content asset file pointer points to an asset
     * file, otherwise false.
     */
    @SuppressWarnings("ConstantConditions")
    public boolean hasAsset() {
        return Utils.notEmpty(assetBytes) || (Utils.notEmpty(asset) && (asset.startsWith(ASSET_SCHEME) || asset.startsWith(FILE_SCHEME)));
    }

}
