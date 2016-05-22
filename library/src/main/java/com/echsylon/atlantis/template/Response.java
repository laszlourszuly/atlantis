package com.echsylon.atlantis.template;

import android.content.Context;

import com.echsylon.atlantis.internal.Utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

/**
 * This class contains all the data required by the {@link com.echsylon.atlantis.Atlantis Atlantis}
 * to serve a response.
 */
public class Response extends HeaderContainer implements Serializable {
    private static final String ASSET_SCHEME = "asset://";
    public static final Response EMPTY = new Response();

    /**
     * This class represents a status code on a response.
     */
    private static final class Code {
        private final Integer code = null;
        private final String name = null;
    }

    private final Code responseCode = null;
    private final String mime = null;
    private final String text = null;
    private final String asset = null;
    private final Integer delay = null;
    private final Integer maxDelay = null;

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
     * @return The MIME type.
     */
    public String mimeType() {
        return mime;
    }

    /**
     * Returns the response body string.
     *
     * @return The content or null.
     */
    public String content() {
        return text;
    }

    /**
     * Returns the response asset content as an array of bytes. It's up to the caller to decide how
     * to further process the result.
     *
     * @param context The context the asset is to be opened from.
     * @return The asset byte array, may be empty but never null.
     */
    public byte[] asset(Context context) throws IOException {
        return Utils.readAsset(context, asset);
    }

    /**
     * Returns the amount of milliseconds to wait before serving this particular response.
     * <p>
     * If only the "delay" variable exists, then that value is returned.
     * <p>
     * If both "delay" and "maxDelay" are given, then a different random number between those values
     * is returned every time this method is called.
     * <p>
     * If only "maxDelay" is given then a different random number between zero (0) and "maxDelay" is
     * returned every time this method is called.
     * <p>
     * If neither "delay" nor "maxDelay" is given, then zero (0) is returned.
     * <p>
     * A "maxDelay" less than "delay" is considered illegal and will cause an exception to be
     * thrown.
     *
     * @return Number of milliseconds. Zero (0) means "no delay".
     * @throws IllegalArgumentException if "maxDelay" is less than "delay".
     */
    @SuppressWarnings("ConstantConditions")
    public long delay() throws IllegalArgumentException {
        if (maxDelay == null || maxDelay.equals(delay))
            return Utils.getNative(delay, 0);

        int min = Utils.getNative(delay, 0);
        int max = Utils.getNative(maxDelay, 0);
        if (max < min)
            throw new IllegalArgumentException("'maxDelay' mustn't be less than 'delay'");

        return new Random().nextInt(max - min) + min;
    }

    /**
     * Returns a flag telling whether the response has a non-empty content text.
     *
     * @return Boolean true if the content text is not a null pointer or an empty string, otherwise
     * false.
     */
    public boolean hasContent() {
        return Utils.notEmpty(text);
    }

    /**
     * Returns a flag telling whether the response has a valid, non-empty asset file pointer. This
     * flag doesn't say anything about the actual content in such an asset file which very well may
     * be empty.
     *
     * @return Boolean true if the content asset file pointer is not a null pointer or an empty
     * string and that it points to an asset file (i.e. starts with "asset://"), otherwise false.
     */
    @SuppressWarnings("ConstantConditions")
    public boolean hasAsset() {
        return Utils.notEmpty(asset) && asset.startsWith(ASSET_SCHEME);
    }

}
