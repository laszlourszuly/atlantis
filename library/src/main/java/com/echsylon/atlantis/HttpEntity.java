package com.echsylon.atlantis;

import java.util.HashMap;
import java.util.Map;

import static com.echsylon.atlantis.internal.Utils.notEmpty;

/**
 * This class is responsible for holding any common type of data for HTTP
 * entities like requests and responses. An example of such type of data could
 * be headers.
 */
class HttpEntity {
    protected Map<String, String> headers = null;

    /**
     * Returns a map of all currently held headers. The returned map is a copy
     * of the internal headers container, hence making changes to it won't
     * affect the internal state.
     *
     * @return The map of request header.
     */
    public Map<String, String> headers() {
        HashMap<String, String> result = new HashMap<>();
        if (headers != null)
            for (Map.Entry<String, String> entry : headers.entrySet())
                result.put(entry.getKey(), entry.getValue());

        return result;
    }

    /**
     * Returns a header value associated with a key or null if no value matches.
     *
     * @param key The key to look for.
     * @return A corresponding string value or null.
     */
    public String header(String key) {
        return headers != null && notEmpty(key) ?
                headers.get(key) :
                null;
    }

}
