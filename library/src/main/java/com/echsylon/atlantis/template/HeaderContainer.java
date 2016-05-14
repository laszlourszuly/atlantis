package com.echsylon.atlantis.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is responsible for holding any header data. This feature is known to be part of both
 * {@link Request} and {@link Response} objects, and is therefore isolated in a separate class.
 */
abstract class HeaderContainer {

    /**
     * The raw array of headers.
     */
    protected final Header[] headers = null;

    /**
     * Returns a list of all currently held headers. The returned list is a copy of the internal
     * headers container, hence making changes to it won't affect the internal state.
     *
     * @return The list of request header.
     */
    @SuppressWarnings("ConstantConditions")
    public List<Header> headers() {
        ArrayList<Header> result = new ArrayList<>();

        // ArrayList#addAll() will internally do an "arrayCopy" which will kind of mutate our own
        // headers field (would anyone perform changes to the returned list from this method, then
        // our internal header collection would not be affected by that).
        if (headers != null)
            result.addAll(Arrays.asList(headers));

        return result;
    }

    /**
     * Returns all response headers with the given key.
     *
     * @param key The key of the headers to filter out.
     * @return The list of response headers.
     */
    @SuppressWarnings("ConstantConditions")
    public List<Header> headers(String key) {
        List<Header> tmp = new ArrayList<>();

        if (headers == null || key == null || key.isEmpty())
            return tmp;

        for (Header header : headers)
            if (key.equals(header.key()))
                tmp.add(header);

        return new ArrayList<>(tmp);
    }

}
