package com.echsylon.atlantis;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import okio.Buffer;
import okio.Sink;
import okio.Source;

import static com.echsylon.atlantis.LogUtils.info;

/**
 * This is a convenience class, holding helper methods for "every day tasks".
 */
@SuppressWarnings({"WeakerAccess", "unused"})
class Utils {

    /**
     * Tries to close a closeable object. Silently consumes any exceptions.
     *
     * @param closeable The thing to close.
     */
    static void closeSilently(Closeable closeable) {
        if (closeable == null)
            return;

        try {
            closeable.close();
        } catch (IOException e) {
            info(e, "Couldn't close closable");
        }
    }

    /**
     * Tries to close a socket. Consumes any {@code IOException}s with a log
     * print.
     *
     * @param socket The socket to close.
     */
    static void closeSilently(Socket socket) {
        if (socket == null)
            return;

        try {
            socket.close();
        } catch (IOException e) {
            info(e, "Couldn't close socket");
        }
    }

    /**
     * Tries to close a server socket. Consumes any {@code IOException}s with a
     * log print.
     *
     * @param serverSocket The server socket to close.
     */
    static void closeSilently(ServerSocket serverSocket) {
        if (serverSocket == null)
            return;

        try {
            serverSocket.close();
        } catch (IOException e) {
            info(e, "Couldn't close server socket");
        }
    }

    /**
     * Tries to close a source. Consumes any {@code IOException}s with a log
     * print.
     *
     * @param source The source to close.
     */
    static void closeSilently(Source source) {
        if (source == null)
            return;

        try {
            source.close();
        } catch (IOException e) {
            info(e, "Couldn't close source");
        }
    }

    /**
     * Tries to close a sink. Consumes any {@code IOException}s with a log
     * print.
     *
     * @param sink The sink to close.
     */
    static void closeSilently(Sink sink) {
        if (sink == null)
            return;

        try {
            sink.close();
        } catch (IOException e) {
            info(e, "Couldn't close sink");
        }
    }

    /**
     * Tries to close a buffer.
     *
     * @param buffer The buffer to close.
     */
    static void closeSilently(Buffer buffer) {
        if (buffer == null)
            return;

        buffer.close();
    }

    /**
     * Tries to put the calling thread to sleep for a given amount of time.
     * There is no guarantee that the requested time of sleep can be honored.
     * Any interruptions will be noted but respectfully ignored.
     *
     * @param millis The amount of milliseconds to try to sleep.
     */
    static void sleepSilently(final long millis) {
        long mark = System.currentTimeMillis();
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            info(e, "Couldn't sleep properly: %s, expected %s",
                    System.currentTimeMillis() - mark, millis);
        }
    }

    /**
     * Returns a native value, representing the value of the given object.
     *
     * @param value    The object to parse the native value from.
     * @param fallback The value to return if the object is a null pointer.
     * @return The native value.
     */
    static boolean getNative(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }


    /**
     * Returns a native value, representing the value of the given object.
     *
     * @param value    The object to parse the native value from.
     * @param fallback The value to return if the object is a null pointer.
     * @return The native value.
     */
    static float getNative(Float value, float fallback) {
        return value != null ? value : fallback;
    }


    /**
     * Returns a native value, representing the value of the given object.
     *
     * @param value    The object to parse the native value from.
     * @param fallback The value to return if the object is a null pointer.
     * @return The native value.
     */
    static int getNative(Integer value, int fallback) {
        return value != null ? value : fallback;
    }


    /**
     * Returns a native value, representing the value of the given object.
     *
     * @param value    The object to parse the native value from.
     * @param fallback The value to return if the object is a null pointer.
     * @return The native value.
     */
    static long getNative(Long value, long fallback) {
        return value != null ? value : fallback;
    }


    /**
     * Returns one of the two given object references, based on if the first one
     * is a null pointer or not.
     *
     * @param object   The object to test if null pointer.
     * @param fallback The reference to return if {@code object} is a null
     *                 pointer.
     * @param <T>      The type of object to test and return.
     * @return The {@code object} param if not null, otherwise the {@code
     * fallback} param.
     */
    static <T> T getNonNull(T object, T fallback) {
        return object != null ? object : fallback;
    }

    /**
     * Tests if any of the given strings is a null pointer or has no length.
     *
     * @param strings The strings to test.
     * @return Boolean true if a null pointer or no arguments is provided, or if
     * at least one of the given strings is a null pointer or has no length,
     * false otherwise.
     */
    static boolean isAnyEmpty(String... strings) {
        if (strings == null || isEmpty(strings))
            return true;

        for (String string : strings)
            if (isEmpty(string))
                return true;

        return false;
    }

    /**
     * Tests if the given list is a null pointer or has no length.
     *
     * @param list The list to test.
     * @return Boolean true if the given list is a null pointer or has no
     * length, false otherwise.
     */
    static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    /**
     * Tests if the given object array is a null pointer or has no length.
     *
     * @param array The object array to test.
     * @return Boolean true if the given object array is a null pointer or has
     * no length, false otherwise.
     */
    static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Tests if the given string is a null pointer or has no length.
     *
     * @param string The string to test.
     * @return Boolean true if the given string is a null pointer or has no
     * length, false otherwise.
     */
    static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Tests if the given byte array is a null pointer or has no length.
     *
     * @param byteArray The byte array to test.
     * @return Boolean true if the given byte array is a null pointer or has no
     * length, false otherwise.
     */
    static boolean isEmpty(byte[] byteArray) {
        return byteArray == null || byteArray.length == 0;
    }

    /**
     * The inverse of {@link #isAnyEmpty(String...)}.
     *
     * @param strings The strings to test.
     * @return Boolean false if a null pointer or no arguments are provided, or
     * if at least one of the given strings is a null pointer or has no length,
     * true otherwise.
     */
    static boolean notAnyEmpty(String... strings) {
        return !isAnyEmpty(strings);
    }

    /**
     * The inverse of {@link #isEmpty(List)}.
     *
     * @param list The list to validate.
     * @return Boolean true if the list isn't a null pointer or empty, false
     * otherwise.
     */
    static boolean notEmpty(List<?> list) {
        return !isEmpty(list);
    }

    /**
     * The inverse of {@link #isEmpty(Object[])}.
     *
     * @param array The array to validate.
     * @return Boolean true if the array isn't a null pointer or empty, false
     * otherwise.
     */
    static boolean notEmpty(Object[] array) {
        return !isEmpty(array);
    }

    /**
     * The inverse of {@link #isEmpty(String)}.
     *
     * @param string The string to validate.
     * @return Boolean true if the string isn't a null pointer or empty, false
     * otherwise.
     */
    static boolean notEmpty(String string) {
        return !isEmpty(string);
    }

    /**
     * The inverse of {@link #isEmpty(byte[])}.
     *
     * @param byteArray The byte array to test.
     * @return Boolean true if the string isn't a null pointer or empty, false
     * otherwise.
     */
    static boolean notEmpty(byte[] byteArray) {
        return !isEmpty(byteArray);
    }

    /**
     * Tries to silently parse a string into a boolean value.
     *
     * @param string   The string to parse.
     * @param fallback The fallback value, would something go wrong.
     * @return The parsed boolean value or {@param fallback} on error.
     */
    static boolean parseBoolean(String string, boolean fallback) {
        if (isEmpty(string))
            return fallback;

        try {
            return Boolean.valueOf(string);
        } catch (NumberFormatException e) {
            info(e, "Couldn't parse '%s' as boolean. Falling back to %s", string, fallback);
            return fallback;
        }
    }

    /**
     * Tries to silently parse a string into a float value.
     *
     * @param string   The string to parse.
     * @param fallback The fallback value, would something go wrong.
     * @return The parsed float value or {@param fallback} on error.
     */
    static float parseFloat(String string, float fallback) {
        if (isEmpty(string))
            return fallback;

        try {
            return Float.valueOf(string);
        } catch (NumberFormatException e) {
            info(e, "Couldn't parse '%s' as float. Falling back to %s", string, fallback);
            return fallback;
        }
    }

    /**
     * Tries to silently parse a string into a decimal long value.
     *
     * @param string   The string to parse.
     * @param fallback The fallback value, would something go wrong.
     * @return The parsed int value or {@param fallback} on error.
     */
    static int parseInt(String string, int fallback) {
        if (isEmpty(string))
            return fallback;

        try {
            return Integer.valueOf(string, 10);
        } catch (NumberFormatException e) {
            info(e, "Couldn't parse '%s' as int. Falling back to %s", string, fallback);
            return fallback;
        }
    }

    /**
     * Tries to silently parse a string into a decimal long value.
     *
     * @param string   The string to parse.
     * @param fallback The fallback value, would something go wrong.
     * @return The parsed long value or {@param fallback} on error.
     */
    static long parseLong(String string, long fallback) {
        if (isEmpty(string))
            return fallback;

        try {
            return Long.valueOf(string, 10);
        } catch (NumberFormatException e) {
            info(e, "Couldn't parse '%s' as long. Falling back to %s", string, fallback);
            return fallback;
        }
    }
}
