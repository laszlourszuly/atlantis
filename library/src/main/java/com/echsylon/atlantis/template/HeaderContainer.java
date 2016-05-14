package com.echsylon.atlantis.template;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for holding any header data. This feature is known to be part of both
 * {@link Request} and {@link Response} objects, and is therefore isolated in a separate class.
 */
abstract class HeaderContainer {

    /**
     * The raw array of headers.
     */
    protected final Map<String, String> headers = null;

    /**
     * Returns a list of all currently held headers. The returned list is a copy of the internal
     * headers container, hence making changes to it won't affect the internal state.
     *
     * @return The list of request header.
     */
    @SuppressWarnings("ConstantConditions")
    public Map<String, String> headers() {
        HashMap<String, String> result = new HashMap<>();

        // ArrayList#addAll() will internally do an "arrayCopy" which will kind of mutate our own
        // headers field (would anyone perform changes to the returned list from this method, then
        // our internal header collection would not be affected by that).
        if (headers != null)
            for (Map.Entry<String, String> entry : headers.entrySet())
                result.put(entry.getKey(), entry.getValue());

        return result;
    }

}
