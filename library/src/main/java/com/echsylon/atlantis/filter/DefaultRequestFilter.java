package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.Request;
import com.echsylon.atlantis.internal.UrlUtils;
import com.echsylon.atlantis.internal.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

/**
 * This class implements the default behaviour of a {@link Request.Filter}. It returns the first
 * request available that matches all hints, or null if there are no requests to pick from or no
 * request could be matched.
 */
public class DefaultRequestFilter implements Request.Filter {

    /**
     * Returns the first request that matches all given hints.
     *
     * @param requests All available requests. Returns null if empty (or null).
     * @param url      The url giving a hint of which request to find. Must not be null.
     * @param method   The corresponding request method to filter on. Must not be null.
     * @param headers  The minimum set of headers that must match. May be null, in which case
     *                 headers are not matched.
     * @return The first matching request template or null.
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
            if (!urlMatchesPattern(url, request.url()))
                if (!urlMatchesReference(url, request.url()))
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

    // Performs a regex validation on the given url. Returns true if the url passes the test, false
    // otherwise.
    private boolean urlMatchesPattern(String url, String regex) {
        StringBuilder stringBuilder = new StringBuilder();
        String path = UrlUtils.getPath(url);
        if (Utils.notEmpty(path))
            stringBuilder.append(path);

        String query = UrlUtils.getQuery(url);
        if (Utils.notEmpty(query))
            stringBuilder.append(query);

        String fragment = UrlUtils.getFragment(url);
        if (Utils.notEmpty(fragment))
            stringBuilder.append(fragment);

        try {
            String test = stringBuilder.toString();
            return test.matches(regex);
        } catch (PatternSyntaxException | NullPointerException e) {
            return false;
        }
    }

    // Validates if the two given urls have the same significant parts. Significant parts in this
    // context are:
    //      1. Path
    //      2. Query parameters (keys and values, but not order)
    //      3. Fragment (the string after the first #-sign)
    private boolean urlMatchesReference(String url, String reference) {
        // Compare the paths
        String path1 = UrlUtils.getPath(url);
        String path2 = UrlUtils.getPath(reference);
        if (!path1.equals(path2))
            return false;

        // Compare the query parameters (order not matched)
        List<String> query1 = Arrays.asList(UrlUtils.getQuery(url).split("&"));
        List<String> query2 = Arrays.asList(UrlUtils.getQuery(reference).split("&"));
        if (query1.size() != query2.size() || !query1.containsAll(query2))
            return false;

        // Compare the url fragments
        String fragment1 = UrlUtils.getFragment(url);
        String fragment2 = UrlUtils.getFragment(reference);
        if (!fragment1.equals(fragment2))
            return false;

        // If we've reached this point, the two urls are considered to match.
        return true;
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

}
