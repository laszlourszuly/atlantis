package com.echsylon.atlantis;

/**
 * This class abstracts away the console logging capabilities from the Atlantis
 * logic. Atlantis can only log "info" messages (to enable some traceability to
 * the developer) and it's then up to this implementation how and at what debug
 * level that message is logged.
 */
class LogUtils {
    private static final String TAG = "ATLANTIS: ";

    /**
     * Sends a simple message to the log.
     *
     * @param message The message to send.
     */
    static void info(String message) {
        System.out.println(TAG + message);
    }

    /**
     * Composes a message from a pattern and corresponding arguments and sends
     * the result to the log.
     *
     * @param pattern The message pattern.
     * @param args    The corresponding message arguments.
     */
    static void info(String pattern, Object... args) {
        System.out.println(TAG + String.format(pattern, args));
    }

    /**
     * Sends an exception to the log.
     *
     * @param error The exception.
     */
    static void info(Throwable error) {
        if (error != null) {
            error.printStackTrace();
        }
    }

    /**
     * Sends a message, along with an exception, to the log.
     *
     * @param error   The exception.
     * @param message The message.
     */
    static void info(Throwable error, String message) {
        info(message);
        info(error);
    }

    /**
     * Composes a message from a pattern and corresponding arguments and sends
     * the result, along with an exception, to the log.
     *
     * @param error   The exception.
     * @param pattern The message pattern.
     * @param args    The corresponding message arguments.
     */
    static void info(Throwable error, String pattern, Object... args) {
        info(pattern, args);
        info(error);
    }
}
