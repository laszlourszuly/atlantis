package com.echsylon.atlantis.template;

import com.echsylon.atlantis.Atlantis;
import com.echsylon.atlantis.internal.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a request template as the {@link Atlantis} local web server expects it. It
 * will use this template when trying to identify which response to serve to a user provided
 * request.
 */
public final class Request implements Serializable {
    private final String headers = null;
    private final String method = null;
    private final String url = null;
    private final Response[] responses = null;

    // Intentionally hidden constructor.
    private Request() {
    }

    /**
     * Returns all the headers for this request.
     *
     * @return The list of request header.
     */
    public List<Header> headers() {
        List<Header> result = new ArrayList<>();

        //noinspection ConstantConditions
        if (Utils.isEmpty(headers))
            return result;

        // Separate the headers from each other.
        String[] splitHeaders = headers.split("\n");
        for (String header : splitHeaders) {
            int firstIndex = header.indexOf(':');

            if (firstIndex != -1) {
                // Separate the key from value
                String key = header.substring(0, firstIndex);
                String value = header.substring(firstIndex + 1);
                result.add(new Header(key, value));
            }
        }

        return result;
    }

    /**
     * Returns the method of this request.
     *
     * @return The request method.
     */
    public String method() {
        return method;
    }

    /**
     * Returns the request url.
     *
     * @return The request url.
     */
    public String url() {
        return url;
    }

    /**
     * Returns the corresponding response for this request.
     *
     * @return The response.
     */
    @SuppressWarnings("ConstantConditions")
    public Response response() {
        return responses != null && responses.length > 0 ?
                responses[0] :
                Response.EMPTY;
    }

}
