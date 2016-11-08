package com.echsylon.atlantis.internal;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a convenience class, holding helper methods for URL related
 * operations. All methods in this class expect the urls to follow the below
 * pattern:
 * <p>
 * [scheme://][[user:password@]host[:port]][/path][?query][#fragment]
 */
public class UrlUtils {
    private static final String SCHEMA_SEPARATOR = "://";

    /**
     * Returns the path part of the given url. The path is considered to be
     * everything between the first single "/" character (included) and the "?"
     * query separator sign (excluded), or the "#" fragment separator sign if
     * there is no query part.
     *
     * @param url The url to extract the path part from.
     * @return The extracted path or an empty string. Never null.
     */
    public static String getPath(String url) {
        int schemaSeparatorStart = url.indexOf(SCHEMA_SEPARATOR);
        int pathStart = schemaSeparatorStart != -1 ?
                url.indexOf("/", schemaSeparatorStart + SCHEMA_SEPARATOR.length()) :
                url.indexOf("/");

        if (pathStart == -1)
            return "";

        // Try to find the end of the path. It either ends where the query part starts, or, if there
        // is no query, where the fragment starts, or, if there is no fragment, where the url ends.
        int pathEnd = url.indexOf("?", pathStart);
        if (pathEnd == -1)
            pathEnd = url.indexOf("#", pathStart);
        if (pathEnd == -1)
            pathEnd = url.length();

        return url.substring(pathStart, pathEnd);
    }

    /**
     * Returns the query part of the given url. The query part is considered to
     * be everything between "?" query separator sign (included) and the "#"
     * fragment separator sign (excluded).
     *
     * @param url The url to extract the query part from.
     * @return The extracted query or an empty string. Never null.
     */
    public static String getQuery(String url) {
        int queryStart = url.indexOf("?");
        if (queryStart == -1)
            return "";

        // Try to find the end of the query string. It ends where the fragment starts, or, if there
        // is no fragment or the fragment is put before the query (valid?), where the url ends.
        int queryEnd = url.indexOf("#", queryStart);
        if (queryEnd == -1)
            queryEnd = url.length();

        return url.substring(queryStart, queryEnd);
    }

    /**
     * Returns the fragment part of the given url. The fragment part is
     * considered to be everything after the "#" fragment separator sign
     * (included).
     *
     * @param url The url to extract the fragment part from.
     * @return The extracted fragment or an empty string. Never null.
     */
    public static String getFragment(String url) {
        int fragmentStart = url.indexOf("#");
        return fragmentStart != -1 ?
                url.substring(fragmentStart) :
                "";
    }

    /**
     * Extracts the response code from an {@link HttpURLConnection}.
     *
     * @param connection The HTTP connection.
     * @return The status code or zero (0).
     */
    public static int getResponseCode(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (NullPointerException | IOException e) {
            return 0;
        }
    }

    /**
     * Extracts the response message from an {@link HttpURLConnection}.
     *
     * @param connection The HTTP connection.
     * @return The response message, or an empty string. Never null.
     */
    public static String getResponseMessage(HttpURLConnection connection) {
        try {
            return connection.getResponseMessage();
        } catch (NullPointerException | IOException e) {
            return "";
        }
    }

    /**
     * Extracts the response headers from an {@link HttpURLConnection}.
     *
     * @param connection The HTTP connection.
     * @return A map of headers. May be empty but never null.
     */
    public static Map<String, String> getResponseHeaders(HttpURLConnection connection) {
        HashMap<String, String> result = new HashMap<>();
        if (connection == null)
            return result;

        int count = connection.getHeaderFields().size();
        for (int i = 0; i < count; i++) {
            String key = connection.getHeaderFieldKey(i);
            String value = connection.getHeaderField(i);

            if (Utils.notAnyEmpty(key, value))
                result.put(key, value);
        }
        return result;
    }

    /**
     * Extracts the response body from an {@link HttpURLConnection}.
     *
     * @param connection The HTTP connection.
     * @return The response body as a byte array. May be empty but never null.
     */
    public static byte[] getResponseBody(HttpURLConnection connection) {
        byte[] bytes = new byte[0];
        if (connection == null)
            return bytes;

        InputStream inputStream = null;
        try {
            int statusCode = connection.getResponseCode();
            inputStream = statusCode < 200 || statusCode > 399 ?
                    connection.getErrorStream() :
                    connection.getInputStream();
            bytes = ByteStreams.toByteArray(inputStream);
        } catch (IOException e) {
            // Respectfully ignore any exceptions.
        } finally {
            Utils.closeSilently(inputStream);
        }

        return bytes;
    }

    /**
     * Extracts the response content type from an {@link HttpURLConnection}.
     *
     * @param connection The HTTP connection.
     * @return The content type or an empty string. Never null.
     */
    public static String getResponseMimeType(HttpURLConnection connection) {
        if (connection == null)
            return "";

        String contentType = connection.getContentType();
        return contentType != null ?
                contentType :
                "";
    }

    /**
     * Tries to close an {@link HttpURLConnection}. Silently consumes any
     * exceptions.
     *
     * @param connection The connection to close.
     */
    public static void closeSilently(HttpURLConnection connection) {
        if (connection == null)
            return;

        connection.disconnect();
    }

}
