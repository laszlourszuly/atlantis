package com.echsylon.atlantis;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static com.echsylon.atlantis.Utils.notAnyEmpty;
import static com.echsylon.atlantis.Utils.parseBoolean;
import static com.echsylon.atlantis.Utils.parseInt;
import static com.echsylon.atlantis.Utils.parseLong;

/**
 * This class holds user configured server behavior. These settings are not
 * related to the HTTP request/response protocol but rather reflect mocked
 * physical characteristics of a remote server.
 */
@SuppressWarnings("WeakerAccess")
class SettingsManager extends LinkedHashMap<String, String> {
    // Response body delivery throttle
    static final String FOLLOW_REDIRECTS = "followRedirects";
    static final String THROTTLE_BYTE_COUNT = "throttleByteCount";
    static final String THROTTLE_MIN_DELAY_MILLIS = "throttleMinDelayMillis";
    static final String THROTTLE_MAX_DELAY_MILLIS = "throttleMaxDelayMillis";

    @Override
    public String put(String key, String value) {
        return notAnyEmpty(key, value) ?
                super.put(key, value) :
                null;
    }

    @Override
    public String putIfAbsent(String key, String value) {
        return !containsKey(key) ?
                put(key, value) :
                get(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> settings) {
        for (Entry<? extends String, ? extends String> entry : settings.entrySet())
            put(entry.getKey(), entry.getValue());
    }

    /**
     * Returns a flag telling whether a response should automatically follow any
     * redirects or not.
     *
     * @return Boolean true if a redirect should be followed, false otherwise.
     */
    boolean followRedirects() {
        return parseBoolean(get(FOLLOW_REDIRECTS), true);
    }

    /**
     * Returns the number of bytes from the response body to send at a time as a
     * "package".
     *
     * @return The desired response body package size.
     */
    long throttleByteCount() {
        return parseLong(get(THROTTLE_BYTE_COUNT), Long.MAX_VALUE);
    }

    /**
     * Returns a random amount of milliseconds for each time this method is
     * called. The delay will be between {@code throttleMinDelayMillis} and
     * {@code throttleMaxDelayMillis} and never less than zero.
     *
     * @return A random delay in milliseconds.
     */
    long throttleDelayMillis() {
        int min = Math.max(0, parseInt(get(THROTTLE_MIN_DELAY_MILLIS), 0));
        int max = Math.max(0, parseInt(get(THROTTLE_MAX_DELAY_MILLIS), 0));

        return max > min ?
                new Random().nextInt((max - min)) + min :
                min;
    }
}
