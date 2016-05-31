package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.Request;
import com.echsylon.atlantis.internal.UrlUtils;
import com.echsylon.atlantis.internal.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class implements the default behaviour of a {@link Request.Filter}. It returns the first
 * request available that matches all hints, or null if there are no requests to pick from or no
 * request could be matched.
 */
public class DefaultRequestFilter implements Request.Filter {

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
        String path1 = UrlUtils.getPath(url1);
        String path2 = UrlUtils.getPath(url2);
        if (!path1.equals(path2))
            return false;

        // Compare the query parameters (order not matched)
        List<String> query1 = Arrays.asList(UrlUtils.getQuery(url1).split("&"));
        List<String> query2 = Arrays.asList(UrlUtils.getQuery(url2).split("&"));
        if (query1.size() != query2.size() || !query1.containsAll(query2))
            return false;

        // Compare the url fragments
        String fragment1 = UrlUtils.getFragment(url1);
        String fragment2 = UrlUtils.getFragment(url2);
        if (!fragment1.equals(fragment2))
            return false;

        // If we've reached this point, the two urls are considered to have the same significant
        // parts.
        return true;
    }


}
