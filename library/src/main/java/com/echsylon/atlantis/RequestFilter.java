package com.echsylon.atlantis;

import com.echsylon.atlantis.template.Request;

import java.util.List;
import java.util.Map;

/**
 * This interface describes the features for filtering out a particular request based on a set of
 * hints describing the desired result.
 */
public interface RequestFilter {

    /**
     * Returns a request object based on the internal filtering logic.
     *
     * @param requests All available requests
     * @param url      The url giving a hint of which request to find.
     * @param method   The corresponding request method to filter on.
     * @param headers  The headers hint.
     * @return The filtered request or null if no match found.
     */
    Request getRequest(List<Request> requests, String url, String method, Map<String, String> headers);

}
