package com.echsylon.atlantis;

import com.echsylon.atlantis.template.Request;
import com.echsylon.atlantis.template.Response;

import java.util.List;

/**
 * This interface describes the features for filtering out a particular response based solely on
 * internal logic.
 */
public interface ResponseFilter {

    /**
     * Returns the next response based on internal state and logic.
     *
     * @param request   The request to find a response for.
     * @param responses All available responses.
     * @return The filtered response or null.
     */
    Response getResponse(Request request, List<Response> responses);

}
