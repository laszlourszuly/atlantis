package com.echsylon.atlantis;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for holding any common type of data for HTTP entities like requests and
 * responses. An example of such type of data could be headers.
 */
class HttpEntity {

    /**
     * The raw map of headers.
     */
    protected Map<String, String> headers = null;


    // Intentionally hidden constructor.
    protected HttpEntity() {
    }

    /**
     * Returns a map of all currently held headers. The returned map is a copy of the internal
     * headers container, hence making changes to it won't affect the internal state.
     *
     * @return The map of request header.
     */
    @SuppressWarnings("ConstantConditions")
    public Map<String, String> headers() {
        HashMap<String, String> result = new HashMap<>();
        if (headers != null)
            for (Map.Entry<String, String> entry : headers.entrySet())
                result.put(entry.getKey(), entry.getValue());

        return result;
    }

}
