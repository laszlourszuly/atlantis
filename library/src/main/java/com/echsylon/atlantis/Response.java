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

import static com.echsylon.atlantis.internal.Utils.notEmpty;

/**
 * This class contains all the data required by {@link com.echsylon.atlantis.Atlantis
 * Atlantis} to serve a response.
 */
@SuppressWarnings("WeakerAccess")
public class Response extends HttpEntity implements Serializable {
    private static final String ASSET_SCHEME = "asset://";
    private static final String FILE_SCHEME = "file://";

    /**
     * This class offers means of building a mocked response configuration
     * directly from code (as opposed to configure one in a JSON asset or file).
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder {
        private final Response response;

        /**
         * Creates a new builder based on an uninitialized response object.
         */
        public Builder() {
            response = new Response();
        }

        /**
         * Creates a new builder based on the given response object.
         *
         * @param response The response object to build on. Must not be null.
         */
        public Builder(Response response) {
            this.response = response;
        }

        /**
         * Adds a header to the response being built. Doesn't add anything if
         * the {@code key} is empty (null pointers are considered empty).
         *
         * @param key   The header key.
         * @param value The header value.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withHeader(String key, String value) {
            if (notEmpty(key)) {
                if (response.headers == null)
                    response.headers = new HashMap<>();

                response.headers.put(key, value);
            }

            return this;
        }

        /**
         * Adds all key/value pairs with a non-empty key from the given map.
         *
         * @param headers The headers to copy keys and values from.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withHeaders(Map<String, String> headers) {
            if (headers != null) {
                if (response.headers == null)
                    response.headers = new HashMap<>();

                for (Map.Entry<String, String> entry : headers.entrySet())
                    if (notEmpty(entry.getKey()))
                        response.headers.put(entry.getKey(), entry.getValue());
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
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withStatus(int code, String name) {
            if (response.responseCode == null)
                response.responseCode = new Code();

            response.responseCode.code = code;
            response.responseCode.name = name;
            return this;
        }

        /**
         * Sets the MIME type of this response. This method doesn't validate the
         * input.
         *
         * @param mimeType The new MIME type.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withMimeType(String mimeType) {
            response.mime = mimeType;
            return this;
        }

        /**
         * Sets the plain text content of this response. This method doesn't
         * validate the input or that it matches any set MIME type.
         *
         * @param content The new plain text content.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withContent(String content) {
            response.text = content;
            return this;
        }

        /**
         * Sets the asset file path of this response.
         *
         * @param assetFile The new asset File pointer.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withAsset(File assetFile) {
            response.asset = assetFile != null ?
                    FILE_SCHEME + assetFile.getAbsolutePath() :
                    null;
            return this;
        }

        /**
         * Sets the asset resource path of this response.
         *
         * @param assetPath The new asset path.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withAsset(String assetPath) {
            response.asset = notEmpty(assetPath) ?
                    !assetPath.startsWith(ASSET_SCHEME) && !assetPath.startsWith(FILE_SCHEME) ?
                            ASSET_SCHEME + assetPath :
                            assetPath :
                    null;
            return this;
        }

        /**
         * Sets the asset resource bytes of this response.
         *
         * @param assetBytes The new asset as a byte array.
         * @return This builder instance, allowing chaining of method calls.
         */
        public Builder withAsset(byte[] assetBytes) {
            response.assetBytes = assetBytes;
            return this;
        }

        /**
         * Sets the default delay for this response, disabling the max delay.
         *
         * @param milliseconds The new amount of milliseconds to delay this
         *                     response.
         * @return This builder instance, allowing chaining of method calls.
         * @see Response#delay() for details on how delay boundaries are
         * interpreted.
         */
        public Builder withDelay(int milliseconds) {
            response.delay = milliseconds;
            response.maxDelay = null;
            return this;
        }

        /**
         * Sets the random delay metrics for this response.
         *
         * @param milliseconds    The minimum amount of random milliseconds to
         *                        delay this response.
         * @param maxMilliseconds The maximum amount of random milliseconds to
         *                        delay this response.
         * @return This builder instance, allowing chaining of method calls.
         * @see Response#delay() for details on how delay boundaries are
         * interpreted.
         */
        public Builder withDelay(int milliseconds, int maxMilliseconds) {
            response.delay = milliseconds;
            response.maxDelay = maxMilliseconds;
            return this;
        }

        /**
         * Returns a sealed response object which can not be further built on.
         *
         * @return The final response object.
         */
        public Response build() {
            return response;
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
            return Utils.readAsset(context, asset.substring(ASSET_SCHEME.length()));

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
    public int delay() {
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
        return notEmpty(text);
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
        return notEmpty(assetBytes) || (notEmpty(asset) && (asset.startsWith(ASSET_SCHEME) || asset.startsWith(FILE_SCHEME)));
    }

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

}
