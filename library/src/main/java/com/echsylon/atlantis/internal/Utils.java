package com.echsylon.atlantis.internal;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * This is a convenience class, holding helper methods for "every day tasks".
 */
public class Utils {

    /**
     * Tries to close a closeable object. Silently consumes any exceptions.
     *
     * @param closeable The thing to close.
     */
    public static void closeSilently(Closeable closeable) {
        if (closeable == null)
            return;

        try {
            closeable.close();
        } catch (IOException e) {
            // Ignore respectfully and silently.
        }
    }

    /**
     * Returns a native value, representing the value of the given object.
     *
     * @param value    The object to parse the native value from.
     * @param fallback The value to return if the object is a null pointer.
     * @return The native value.
     */
    public static boolean getNative(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }


    /**
     * Returns a native value, representing the value of the given object.
     *
     * @param value    The object to parse the native value from.
     * @param fallback The value to return if the object is a null pointer.
     * @return The native value.
     */
    public static float getNative(Float value, float fallback) {
        return value != null ? value : fallback;
    }


    /**
     * Returns a native value, representing the value of the given object.
     *
     * @param value    The object to parse the native value from.
     * @param fallback The value to return if the object is a null pointer.
     * @return The native value.
     */
    public static int getNative(Integer value, int fallback) {
        return value != null ? value : fallback;
    }


    /**
     * Returns a native value, representing the value of the given object.
     *
     * @param value    The object to parse the native value from.
     * @param fallback The value to return if the object is a null pointer.
     * @return The native value.
     */
    public static long getNative(Long value, long fallback) {
        return value != null ? value : fallback;
    }


    /**
     * Returns one of the two given object references, based on if the first one is a null pointer
     * or not.
     *
     * @param object   The object to test if null pointer.
     * @param fallback The reference to return if {@code object} is a null pointer.
     * @param <T>      The type of object to test and return.
     * @return The {@code object} param if not null, otherwise the {@code fallback} param.
     */
    public static <T> T getNonNull(T object, T fallback) {
        return object != null ? object : fallback;
    }

    /**
     * Tests if the given list is a null pointer or has no length.
     *
     * @param list The list to test.
     * @return Boolean true if the given list is a null pointer or has no length, false otherwise.
     */
    public static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    /**
     * Tests if the given object array is a null pointer or has no length.
     *
     * @param array The object array to test.
     * @return Boolean true if the given object array is a null pointer or has no length, false
     * otherwise.
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Tests if the given string is a null pointer or has no length.
     *
     * @param string The string to test.
     * @return Boolean true if the given string is a null pointer or has no length, false otherwise.
     */
    public static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /**
     * The inverse of {@link #isEmpty(List)}.
     *
     * @param list The list to validate.
     * @return Boolean true if the list isn't a null pointer or empty, false otherwise.
     */
    public static boolean notEmpty(List<?> list) {
        return !isEmpty(list);
    }

    /**
     * The inverse of {@link #isEmpty(Object[])}.
     *
     * @param array The array to validate.
     * @return Boolean true if the array isn't a null pointer or empty, false otherwise.
     */
    public static boolean notEmpty(Object[] array) {
        return !isEmpty(array);
    }

    /**
     * The inverse of {@link #isEmpty(String)}.
     *
     * @param string The string to validate.
     * @return Boolean true if the string isn't a null pointer or empty, false otherwise.
     */
    public static boolean notEmpty(String string) {
        return !isEmpty(string);
    }

}
