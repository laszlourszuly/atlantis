package com.echsylon.atlantis.template;

import android.content.Context;

import com.echsylon.atlantis.Atlantis;
import com.echsylon.atlantis.internal.Utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class contains all the data required by the {@link Atlantis} to serve a response.
 */
public final class Response implements Serializable {
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
    private final Header[] headers = null;
    private final String mime = null;
    private final String text = null;
    private final String asset = null;

    // Intentionally hidden constructor.
    private Response() {
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
     * Returns all the response headers, or an empty list if there are no headers.
     *
     * @return The list of response headers.
     */
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public List<Header> headers() {
        return headers != null ?
                Arrays.asList(headers) :
                Collections.EMPTY_LIST;
    }

    /**
     * Returns all response headers with the given key.
     *
     * @param key The key of the headers to filter out.
     * @return The list of response headers.
     */
    @SuppressWarnings("ConstantConditions")
    public List<Header> headers(String key) {
        List<Header> result = new ArrayList<>();

        if (headers == null || key == null || key.isEmpty())
            return result;

        for (Header header : headers)
            if (key.equals(header.key()))
                result.add(header);

        return result;
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
    public byte[] asset(Context context) {
        try {
            return Utils.readAsset(context, asset);
        } catch (IOException e) {
            return new byte[0];
        }
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
