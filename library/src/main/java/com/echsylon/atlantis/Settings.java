package com.echsylon.atlantis;

/**
 * This class holds user configured server behavior. These settings are note
 * related to the HTTP request/response protocol but rather reflect mocked
 * physical characteristics of a remote server.
 */
class Settings {

    /**
     * This class describes the response body streaming behavior.
     */
    static final class Throttle {
        final long throttleByteCount;
        final long throttleDelayMillis;

        Throttle() {
            this(Long.MAX_VALUE, 1000L);
        }

        Throttle(long throttleByteCount, long throttleDelayMillis) {
            this.throttleDelayMillis = throttleDelayMillis;
            this.throttleByteCount = throttleByteCount;
        }
    }

    private Throttle throttle = null;

    Throttle throttle() {
        return throttle == null ?
                new Throttle() :
                throttle;
    }
}
