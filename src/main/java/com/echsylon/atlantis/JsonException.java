package com.echsylon.atlantis;

/**
 * This instance of a runtime exception represents an error caused while parsing
 * JSON data.
 */
@SuppressWarnings("WeakerAccess")
public class JsonException extends RuntimeException {
    JsonException(Throwable cause) {
        super(cause);
    }
}
