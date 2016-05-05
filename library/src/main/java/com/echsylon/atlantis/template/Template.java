package com.echsylon.atlantis.template;

import com.echsylon.atlantis.Atlantis;
import com.echsylon.atlantis.internal.Utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class contains all request templates the {@link Atlantis} local web server will ever serve.
 * This is the "downloaded Internet".
 */
public final class Template implements Serializable {
    private static final String SCHMEA_SEPARATOR = "://";
    private final Request[] requests = null;

    // Intentionally hidden constructor.
    private Template() {
    }

    /**
     * Tries to find a request template that matches the given method, url and header criteria.
     * Returns the first match.
     *
     * @param url     The url.
     * @param method  The request method.
     * @param headers The request headers.
     * @return The request template that matches the given criteria or null if no match found.
     */
    public Request findRequest(String url, String method, List<Header> headers) {
        //noinspection ConstantConditions
        if (Utils.isEmpty(requests))
            return null;

        for (Request request : requests) {
            // Validate method
            if (!method.equalsIgnoreCase(request.method()))
                continue;

            // Validate significant parts of url
            if (!hasSameSignificantParts(url, request.url()))
                continue;

            // Validate headers
            if (!hasHeaders(headers, request.headers()))
                continue;

            // If we reached this point, we have a match
            return request;
        }

        // There is no match. Try not to cry. Cry a lot.
        return null;
    }

    /**
     * Validates if the two list of headers are the same. The test validates keys and corresponding
     * values, but not their internal order.
     *
     * @param test      The first list of headers.
     * @param reference The second list of headers.
     * @return Boolean true if the two lists contains the same headers, false otherwise.
     */
    private boolean hasHeaders(List<Header> test, List<Header> reference) {
        return reference == null || test == null || test.containsAll(reference);
    }

    /**
     * Validates if the two given urls have the same significant parts. Significant parts in this
     * context are:
     * <pre>
     *      1. Path
     *
     *      2. Query parameters (keys and values, but not order)
     *
     *      3. Fragment (the string after the first #-sign)
     * </pre>
     *
     * @param url1 The reference url.
     * @param url2 The url to compare.
     * @return Boolean true if the significant parts are the same, false otherwise.
     */
    private boolean hasSameSignificantParts(String url1, String url2) {
        // Compare the paths
        String path1 = getPath(url1);
        String path2 = getPath(url2);
        if (!path1.equals(path2))
            return false;

        // Compare the query parameters (order not matched)
        List<String> query1 = getQueryParameters(url1);
        List<String> query2 = getQueryParameters(url2);
        if (query1.size() != query2.size() || !query1.containsAll(query2))
            return false;

        // Compare the url fragments
        String fragment1 = getFragment(url1);
        String fragment2 = getFragment(url2);
        if (!fragment1.equals(fragment2))
            return false;

        // If we've reached this point, the two urls are considered to have the same significant
        // parts.
        return true;
    }

    /**
     * Returns the path part of the given url. The path is considered to be everything between the
     * first single "/" character and the "?" query separator sign, or the "#" fragment separator
     * sign if there is no query part.
     *
     * @param url The url to extract the path from.
     * @return The path part of the url or an empty string if there is no path. Never null.
     */
    private String getPath(String url) {
        int schemaSeparatorStart = url.indexOf(SCHMEA_SEPARATOR);
        int pathStart = schemaSeparatorStart != -1 ?
                url.indexOf("/", schemaSeparatorStart + SCHMEA_SEPARATOR.length()) :
                url.indexOf("/");

        if (pathStart == -1)
            return "";

        // Try to find the end of the path. It either ends where the query part starts, or, if there
        // is no query, where the fragment starts.
        int pathEnd = url.indexOf("?", pathStart);
        if (pathEnd == -1)
            pathEnd = url.indexOf("#", pathStart);
        if (pathEnd == -1)
            pathEnd = url.length();

        return url.substring(pathStart + 1, pathEnd);
    }

    /**
     * Returns the query part of the given url. The query part is considered to be everything
     * between "?" query separator sign and the "#" fragment separator sign.
     *
     * @param url The url to extract the query from.
     * @return The query part of the url or an empty string if there is no query. Never null.
     */
    private List<String> getQueryParameters(String url) {
        int queryStart = url.indexOf("?");
        if (queryStart == -1)
            return Collections.emptyList();

        // Try to find the end of the query string. It ends where the fragment starts.
        int queryEnd = url.indexOf("#", queryStart);
        if (queryEnd == -1)
            queryEnd = url.length();

        return Arrays.asList(url.substring(queryStart + 1, queryEnd).split("&"));
    }

    /**
     * Returns the fragment part of the given url. The fragment part is considered to be everything
     * after the "#" fragment separator sign.
     *
     * @param url The url to extract the fragment from.
     * @return The fragment part of the url or an empty string if there is no fragment. Never null.
     */
    private String getFragment(String url) {
        int fragmentStart = url.indexOf("#");
        return fragmentStart != -1 ?
                url.substring(fragmentStart + 1) :
                "";
    }

}
