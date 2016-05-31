package com.echsylon.atlantis.internal;

import android.content.Context;
import android.content.res.AssetManager;

import com.google.common.io.ByteStreams;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
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
     * Tests if any of the given strings is a null pointer or has no length.
     *
     * @param strings The strings to test.
     * @return Boolean true if a null pointer or no arguments is provided, or if at least one of the
     * given strings is a null pointer or has no length, false otherwise.
     */
    public static boolean isAnyEmpty(String... strings) {
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
     * Tests if the given byte array is a null pointer or has no length.
     *
     * @param byteArray The byte array to test.
     * @return Boolean true if the given byte array is a null pointer or has no length, false
     * otherwise.
     */
    public static boolean isEmpty(byte[] byteArray) {
        return byteArray == null || byteArray.length == 0;
    }

    /**
     * The inverse of {@link #isAnyEmpty(String...)}.
     *
     * @param strings The strings to test.
     * @return Boolean false if a null pointer or no arguments are provided, or if at least one of
     * the given strings is a null pointer or has no length, true otherwise.
     */
    public static boolean notAnyEmpty(String... strings) {
        return !isAnyEmpty(strings);
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

    /**
     * The inverse of {@link #isEmpty(byte[])}.
     *
     * @param byteArray The byte array to test.
     * @return Boolean true if the string isn't a null pointer or empty, false otherwise.
     */
    public static boolean notEmpty(byte[] byteArray) {
        return !isEmpty(byteArray);
    }

    /**
     * Tries to read the entire content of the given asset file.
     *
     * @param context   The context to read the assets from.
     * @param assetName The name of the asset that describes the responses.
     * @return The content of the asset as a byte array.
     * @throws IOException If failed opening the asset input stream.
     */
    public static byte[] readAsset(Context context, String assetName) throws IOException {
        if (context == null || isEmpty(assetName))
            return new byte[0];

        InputStream inputStream = null;
        byte[] bytes;

        try {
            AssetManager assetManager = context.getAssets();
            inputStream = assetManager.open(assetName);
            bytes = ByteStreams.toByteArray(inputStream);
        } finally {
            closeSilently(inputStream);
        }

        return bytes;
    }

    /**
     * Tries to read the entire content of the given input stream.
     *
     * @param inputStream The input stream to read bytes from.
     * @return The content of the input stream as a byte array.
     * @throws IOException If failed reading from the input stream.
     */
    public static byte[] readAsset(InputStream inputStream) throws IOException {
        if (inputStream == null)
            return new byte[0];

        byte[] bytes;
        try {
            bytes = ByteStreams.toByteArray(inputStream);
        } finally {
            closeSilently(inputStream);
        }
        return bytes;
    }


}
