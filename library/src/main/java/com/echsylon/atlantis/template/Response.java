package com.echsylon.atlantis.template;

import com.echsylon.atlantis.Atlantis;
import com.echsylon.atlantis.internal.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class contains all the data required by the {@link Atlantis} to serve
 * a response.
 */
public final class Response implements Serializable {
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
     * @return The content.
     */
    public String content() {
        return text;
    }

}
