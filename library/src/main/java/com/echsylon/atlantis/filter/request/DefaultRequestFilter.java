package com.echsylon.atlantis.filter.request;

import com.echsylon.atlantis.RequestFilter;
import com.echsylon.atlantis.internal.Utils;
import com.echsylon.atlantis.template.Request;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class implements the default behaviour of a {@link RequestFilter}. It returns the first
 * request available that matches all hints, or null if there are no requests to pick from or no
 * request could be matched.
 */
public class DefaultRequestFilter implements RequestFilter {
    private static final String SCHEMA_SEPARATOR = "://";

    /**
     * Returns the first request that matches all given hints.
     *
     * @param requests All available requests
     * @param url      The url giving a hint of which request to find.
     * @param method   The corresponding request method to filter on.
     * @param headers  The minimum set of headers that must match.
     * @return The first matching request or null.
     */
    @Override
    public Request getRequest(List<Request> requests, String url, String method, Map<String, String> headers) {
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

    // Validates if the two list of headers are the same. The test validates keys and corresponding
    // values, but not their internal order. "test" == null is valid and signals a "don't test for
    // headers" case.
    private boolean hasHeaders(Map<String, String> test, Map<String, String> reference) {
        // No headers required. Pass the test.
        if (reference == null || test == null)
            return true;

        // Verify all required headers are present.
        for (Map.Entry<String, String> referenceEntry : reference.entrySet())
            if (!test.containsKey(referenceEntry.getKey()) || !test.get(referenceEntry.getKey()).equals(referenceEntry.getValue()))
                return false;

        return true;
    }

    // Validates if the two given urls have the same significant parts. Significant parts in this
    // context are:
    //      1. Path
    //      2. Query parameters (keys and values, but not order)
    //      3. Fragment (the string after the first #-sign)
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

    // Returns the path part of the given url. The path is considered to be everything between the
    // first single "/" character and the "?" query separator sign, or the "#" fragment separator
    // sign if there is no query part.
    private String getPath(String url) {
        int schemaSeparatorStart = url.indexOf(SCHEMA_SEPARATOR);
        int pathStart = schemaSeparatorStart != -1 ?
                url.indexOf("/", schemaSeparatorStart + SCHEMA_SEPARATOR.length()) :
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

    // Returns the query part of the given url. The query part is considered to be everything
    // between "?" query separator sign and the "#" fragment separator sign.
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

    // Returns the fragment part of the given url. The fragment part is considered to be everything
    // after the "#" fragment separator sign.
    private String getFragment(String url) {
        int fragmentStart = url.indexOf("#");
        return fragmentStart != -1 ?
                url.substring(fragmentStart + 1) :
                "";
    }

}
