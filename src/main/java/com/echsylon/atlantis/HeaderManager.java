package com.echsylon.atlantis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.echsylon.atlantis.Utils.isAnyEmpty;
import static com.echsylon.atlantis.Utils.isEmpty;
import static com.echsylon.atlantis.Utils.join;
import static com.echsylon.atlantis.Utils.notEmpty;

/**
 * This class is responsible for keeping track of any header key/value pairs. It
 * gives convenience hints on certain expected behavior based on which headers
 * are present in the current collection.
 */
@SuppressWarnings("WeakerAccess")
public class HeaderManager {
    private transient final Map<String, List<String>> headers = new LinkedHashMap<>();
    private transient boolean expectedBody = false;
    private transient boolean expectedChunked = false;
    private transient boolean expectedContinue = false;

    /**
     * Replaces any existing headers for a given key with a new value. If no
     * values exists for the key then this method is synonymous with {@link
     * #add(String, String)}.
     *
     * @param key   The header key.
     * @param value The new header value
     */
    void set(final String key, final String value) {
        headers.remove(key);
        add(key, value);
    }

    /**
     * Adds a header key/value pair to the internal collection.
     *
     * @param key   The header key.
     * @param value The corresponding header value.
     */
    void add(final String key, final String value) {
        if (isAnyEmpty(key, value))
            return;

        if (!headers.containsKey(key))
            headers.put(key, new ArrayList<>());

        List<String> values = headers.get(key);
        if (!values.contains(value))
            values.add(value);

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
    }

    /**
     * Adds all header values with a given key to the internal collection.
     *
     * @param key    The header key.
     * @param values The corresponding header values.
     */
    void add(final String key, List<String> values) {
        for (String value : values)
            add(key, value);
    }

    /**
     * Adds all header values for all keys in the given map to the internal
     * collection.
     *
     * @param headers The headers to add.
     */
    void add(final Map<String, List<String>> headers) {
        if (notEmpty(headers))
            for (Map.Entry<String, List<String>> entry : headers.entrySet())
                add(entry.getKey(), entry.getValue());
    }

    /**
     * Adds all header values for a key to the internal collection, but only if
     * there isn't already a value with the same key present.
     *
     * @param key    The header key.
     * @param values The corresponding header values.
     */
    void addIfKeyAbsent(final String key, final List<String> values) {
        if (!headers.containsKey(key))
            add(key, values);
    }

    /**
     * Adds all given header values to the internal collection, but only if
     * there isn't already a value with the same key present.
     *
     * @param headers The header values.
     */
    void addIfKeyAbsent(final Map<String, List<String>> headers) {
        if (notEmpty(headers))
            for (Map.Entry<String, List<String>> entry : headers.entrySet())
                addIfKeyAbsent(entry.getKey(), entry.getValue());
    }

    /**
     * Returns all header values for a given key.
     *
     * @param key The header key.
     * @return All corresponding header values in an unmodifiable list, or an
     * empty list if no values could be found for the given key. Never null.
     */
    public List<String> get(final String key) {
        if (isEmpty(key))
            return Collections.emptyList();

        if (!headers.containsKey(key))
            return Collections.emptyList();

        List<String> values = headers.get(key);
        if (isEmpty(values))
            return Collections.emptyList();

        return Collections.unmodifiableList(values);
    }

    /**
     * Returns the most recently added header value for the given key.
     *
     * @param key The header key.
     * @return The last added corresponding header value, or null if no value
     * could be found for the given key.
     */
    public String getMostRecent(final String key) {
        if (isEmpty(key))
            return null;

        if (!headers.containsKey(key))
            return null;

        List<String> values = headers.get(key);
        if (isEmpty(values))
            return null;

        int lastIndex = values.size() - 1;
        return values.get(lastIndex);
    }

    /**
     * Returns an immutable map with all header values for all header keys. If
     * there are multiple values for a key, these values are merged, separated
     * by a comma, all according to RFC-2616 Section 4.2 "Message Headers".
     *
     * @return All added header keys and values or an empty map if no headers
     * have been added yet.
     */
    public Map<String, String> getAllAsMap() {
        if (isEmpty(headers))
            return Collections.emptyMap();

        final LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet())
            result.put(entry.getKey(), join(", ", entry.getValue()));

        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an immutable multi-map with all header values for all header
     * keys.
     *
     * @return All added header keys and values or an empty map if no headers
     * have been added yet.
     */
    public Map<String, List<String>> getAllAsMultiMap() {
        if (isEmpty(headers))
            return Collections.emptyMap();

        final LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet())
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));

        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an immutable list with all header keys and values. Even list
     * indices should hold a key and odd list indices should hold a value.
     *
     * @return All added header keys and values or an empty list if no headers
     * have been added yet.
     */
    public List<String> getAllAsList() {
        if (isEmpty(headers))
            return Collections.emptyList();

        final ArrayList<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                result.add(key);
                result.add(value);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the number of unique header keys managed by this header manager.
     *
     * @return The number of added keys.
     */
    public int keyCount() {
        return headers.keySet().size();
    }

    /**
     * Returns a boolean flag indicating whether there is a "Content-Length"
     * header with a value greater than 0.
     *
     * @return Boolean true if there is a "Content-Length" header and a
     * corresponding value greater than 0, false otherwise.
     */
    public boolean isExpectedToHaveBody() {
        return expectedBody;
    }

    /**
     * Returns a boolean flag indicating whether there is an "Expect" header
     * with a "100-continue" value.
     *
     * @return Boolean true if there is an "Expect" header and a corresponding
     * "100-Continue" value, false otherwise.
     */
    public boolean isExpectedToContinue() {
        return expectedContinue;
    }

    /**
     * Returns a boolean flag indicating whether there is a "Transfer-Encoding"
     * header with a "chunked" value.
     *
     * @return Boolean true if there is a "Transfer-Encoding" header and a
     * corresponding "chunked" value, false otherwise.
     */
    public boolean isExpectedToBeChunked() {
        return expectedChunked;
    }
}
