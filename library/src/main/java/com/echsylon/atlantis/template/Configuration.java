package com.echsylon.atlantis.template;

import com.echsylon.atlantis.internal.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class contains all request templates the {@link com.echsylon.atlantis.Atlantis Atlantis}
 * local web server will ever serve. This is the "downloaded Internet".
 */
public class Configuration implements Serializable {
    private static final String SCHMEA_SEPARATOR = "://";

    /**
     * This class offers means of building a configuration object directly from code (as opposed to
     * configure one in a JSON asset).
     */
    public static final class Builder extends Configuration {

        /**
         * Adds a request to the configuration being built. Doesn't add null pointers.
         *
         * @param request The request to add.
         * @return This buildable configuration object, allowing chaining of method calls.
         */
        public Builder withRequest(Request request) {
            if (request != null) {
                if (this.requests == null)
                    this.requests = new ArrayList<>();

                requests.add(request);
            }

            return this;
        }

    }

    protected List<Request> requests = null;

    // Intentionally hidden constructor
    private Configuration() {
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
    public Request findRequest(String url, String method, Map<String, String> headers) {
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
