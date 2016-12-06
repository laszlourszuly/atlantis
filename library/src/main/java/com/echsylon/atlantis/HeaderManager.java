package com.echsylon.atlantis;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.echsylon.atlantis.Utils.isAnyEmpty;

/**
 * This class is responsible for keeping track of any header key/value pairs. It
 * gives convenience hints on certain expected behavior based on which headers
 * are present in the current collection.
 */
class HeaderManager extends LinkedHashMap<String, String> {
    private boolean expectedBody = false;
    private boolean expectedChunked = false;
    private boolean expectedContinue = false;

    /**
     * Adds a header key/value pair to the internal collection. Any existing
     * header with the same key will be overwritten.
     *
     * @param key   The key of the header.
     * @param value The corresponding header value.
     * @return Any previous value for the given key.
     */
    @Override
    public String put(String key, String value) {
        if (isAnyEmpty(key, value))
            return null;

        String previous = super.put(key, value);

        if (key.equalsIgnoreCase("expect"))
            expectedContinue = value.equalsIgnoreCase("100-continue");

        if (key.equalsIgnoreCase("transfer-encoding"))
            expectedChunked = value.equalsIgnoreCase("chunked");

        if (key.equalsIgnoreCase("content-length"))
            try {
                expectedBody = Long.parseLong(value) > 0L;
            } catch (NumberFormatException e) {
                expectedBody = false;
            }

        return previous;
    }

    @Override
    public String putIfAbsent(String key, String value) {
        return containsKey(key) ?
                get(key) :
                put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> headers) {
        for (Entry<? extends String, ? extends String> entry : headers.entrySet())
            put(entry.getKey(), entry.getValue());
    }

    /**
     * Returns a boolean flag indicating whether there is a "Content-Length"
     * header with a value greater than 0.
     *
     * @return Boolean true if there is a "Content-Length" header and a
     * corresponding value greater than 0, false otherwise.
     */
    boolean isExpectedToHaveBody() {
        return expectedBody;
    }

    /**
     * Returns a boolean flag indicating whether there is an "Expect" header
     * with a "100-continue" value.
     *
     * @return Boolean true if there is an "Expect" header and a corresponding
     * "100-Continue" value, false otherwise.
     */
    boolean isExpectedToContinue() {
        return expectedContinue;
    }

    /**
     * Returns a boolean flag indicating whether there is a "Transfer-Encoding"
     * header with a "chunked" value.
     *
     * @return Boolean true if there is a "Transfer-Encoding" header and a
     * corresponding "chunked" value, false otherwise.
     */
    boolean isExpectedToBeChunked() {
        return expectedChunked;
    }
}
